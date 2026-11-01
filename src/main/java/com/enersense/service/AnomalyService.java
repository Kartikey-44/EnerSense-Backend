package com.enersense.service;

import com.enersense.config.AnomalyProperties;
import com.enersense.domain.Cursor;
import com.enersense.domain.MeterReading;
import com.enersense.repository.MeterReadingRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides whether the reading under the cursor is unusually high for that household at
 * that hour.
 *
 * <p>The baseline is same-hour: 7pm is compared with previous 7pms. A daily average would
 * flag every evening in a house that cooks dinner.
 *
 * <p>Two conditions must both hold — the reading exceeds the mean by more than
 * {@code zMultiplier} sample standard deviations, <em>and</em> by at least
 * {@code minDeltaWh} in absolute terms. The second is what stops a very steady low-draw
 * meter from alerting on a trivial fluctuation that happens to be statistically large.
 *
 * <p>An episode state machine suppresses repeats: a spike lasting six hours raises one
 * alert, not six. Without it the feature is a nuisance and users mute it.
 */
@Service
@Transactional(readOnly = true)
public class AnomalyService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private final MeterAccess meterAccess;
    private final SimulationService simulation;
    private final MeterReadingRepository readings;
    private final AnomalyProperties properties;
    private final Map<String, Episode> episodes = new ConcurrentHashMap<>();

    public AnomalyService(MeterAccess meterAccess,
                          SimulationService simulation,
                          MeterReadingRepository readings,
                          AnomalyProperties properties) {
        this.meterAccess = meterAccess;
        this.simulation = simulation;
        this.readings = readings;
        this.properties = properties;
    }

    public Assessment current(String consumerId, String meterId) {
        meterAccess.require(consumerId, meterId);
        Cursor cursor = simulation.requireCursor(consumerId, meterId);

        MeterReading reading = readings.findOwnReading(meterId, cursor.readingId())
                .orElseThrow(() -> new NoReadingsException(meterId));
        BigDecimal currentWh = reading.getEnergyConsumptionWh();
        LocalDateTime at = reading.getReadingTimestamp();

        List<MeterReading> baseline = readings.findReleasedAtHour(
                meterId, at.getHour(), cursor.timestamp(), cursor.readingId(),
                Limit.of(properties.maxBaselineDays()));

        String key = key(consumerId, meterId);
        // A cursor that moved backwards means the simulation restarted; the old episode
        // describes a timeline that no longer exists.
        episodes.computeIfPresent(key, (ignored, episode) ->
                episode.lastReadingId() > cursor.readingId() ? null : episode);

        if (baseline.size() < properties.minBaselineDays()) {
            episodes.remove(key);
            return Assessment.learning(consumerId, meterId, at, currentWh);
        }

        Stats stats = Stats.of(baseline);
        BigDecimal threshold = stats.mean().add(properties.zMultiplier().multiply(stats.stdDev()));
        BigDecimal delta = currentWh.subtract(stats.mean());
        boolean unusual = currentWh.compareTo(threshold) > 0
                && delta.compareTo(properties.minDeltaWh()) >= 0;

        boolean firstOfEpisode = transition(key, cursor.readingId(), unusual);

        BigDecimal percentAbove = stats.mean().signum() > 0
                ? delta.multiply(BigDecimal.valueOf(100)).divide(stats.mean(), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new Assessment(consumerId, meterId,
                unusual ? Status.UNUSUAL : Status.NORMAL,
                at,
                currentWh.setScale(3, RoundingMode.HALF_UP),
                stats.mean(),
                delta.setScale(3, RoundingMode.HALF_UP),
                percentAbove,
                firstOfEpisode,
                unusual
                        ? "Consumption is well above this household's usual level for this hour."
                        : null);
    }

    /** @return true when this reading opens a new episode, meaning it is worth notifying */
    private boolean transition(String key, long readingId, boolean unusual) {
        Episode previous = episodes.put(key, new Episode(readingId, unusual));
        return unusual && (previous == null || !previous.unusual());
    }

    /** Forgets episode state. Used by tests and after a simulation restart. */
    public void reset() {
        episodes.clear();
    }

    private static String key(String consumerId, String meterId) {
        return consumerId + '\u0000' + meterId;
    }

    private record Episode(long lastReadingId, boolean unusual) {
    }

    /** Mean and sample standard deviation, with Bessel's correction. */
    record Stats(BigDecimal mean, BigDecimal stdDev) {

        static Stats of(List<MeterReading> sample) {
            int n = sample.size();
            BigDecimal sum = sample.stream()
                    .map(MeterReading::getEnergyConsumptionWh)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mean = sum.divide(BigDecimal.valueOf(n), 3, RoundingMode.HALF_UP);

            if (n < 2) {
                return new Stats(mean, BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            }
            BigDecimal sumSquares = BigDecimal.ZERO;
            for (MeterReading reading : sample) {
                BigDecimal diff = reading.getEnergyConsumptionWh().subtract(mean);
                sumSquares = sumSquares.add(diff.multiply(diff));
            }
            // n - 1, not n: with 3 to 14 observations the population formula understates
            // the spread and the detector would cry wolf.
            BigDecimal variance = sumSquares.divide(BigDecimal.valueOf(n - 1L), 6, RoundingMode.HALF_UP);
            return new Stats(mean, variance.sqrt(MC).setScale(3, RoundingMode.HALF_UP));
        }
    }

    public enum Status { NORMAL, UNUSUAL, LEARNING }

    public record Assessment(String consumerId,
                             String meterId,
                             Status status,
                             LocalDateTime at,
                             BigDecimal currentWh,
                             BigDecimal baselineMeanWh,
                             BigDecimal differenceWh,
                             BigDecimal percentAboveBaseline,
                             boolean newEpisode,
                             String message) {

        static Assessment learning(String consumerId, String meterId, LocalDateTime at, BigDecimal currentWh) {
            return new Assessment(consumerId, meterId, Status.LEARNING, at,
                    currentWh.setScale(3, RoundingMode.HALF_UP), null, null, null, false,
                    "Still learning this household's usual pattern for this hour.");
        }
    }
}

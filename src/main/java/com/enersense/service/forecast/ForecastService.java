package com.enersense.service.forecast;

import com.enersense.config.ForecastProperties;
import com.enersense.domain.Cursor;
import com.enersense.domain.DailyTotal;
import com.enersense.domain.MeterReading;
import com.enersense.repository.MeterReadingRepository;
import com.enersense.service.Aggregations;
import com.enersense.service.DataQuality;
import com.enersense.service.MeterAccess;
import com.enersense.service.NoReadingsException;
import com.enersense.service.SimulationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Predicts the next day's consumption and spreads it across 24 hours.
 *
 * <p>Two steps. A {@link DailyTotalModel} predicts the day's energy; then that total is
 * distributed using the share each hour historically took. The split matters — getting the
 * daily level right and the shape wrong is a different failure from the reverse, and
 * keeping them apart lets the models be swapped and measured independently.
 *
 * <p>Both steps read the <em>same</em> window of days. An earlier version drew the daily
 * level from one week but built the hourly shape from the entire history, so a 7-day
 * forecast was reshaped by a profile spanning months.
 */
@Service
@Transactional(readOnly = true)
public class ForecastService {

    private static final Logger log = LoggerFactory.getLogger(ForecastService.class);
    private static final int HOURS_PER_DAY = 24;

    private final MeterAccess meterAccess;
    private final SimulationService simulation;
    private final MeterReadingRepository readings;
    private final ForecastProperties properties;
    private final Map<ForecastProperties.Strategy, DailyTotalModel> models;

    public ForecastService(MeterAccess meterAccess,
                           SimulationService simulation,
                           MeterReadingRepository readings,
                           ForecastProperties properties) {
        this.meterAccess = meterAccess;
        this.simulation = simulation;
        this.readings = readings;
        this.properties = properties;
        this.models = new EnumMap<>(ForecastProperties.Strategy.class);
        models.put(ForecastProperties.Strategy.PERSISTENCE, new PersistenceModel());
        models.put(ForecastProperties.Strategy.MOVING_AVERAGE, new MovingAverageModel(properties.windowDays()));
        models.put(ForecastProperties.Strategy.EWMA, new EwmaModel(properties.ewmaAlpha()));
    }

    public Forecast tomorrow(String consumerId, String meterId) {
        meterAccess.require(consumerId, meterId);
        Cursor cursor = simulation.requireCursor(consumerId, meterId);

        List<MeterReading> released = readings.findAllReleased(meterId, cursor.timestamp(), cursor.readingId());
        if (released.isEmpty()) {
            throw new NoReadingsException(meterId);
        }

        DataQuality.Report quality = DataQuality.screen(released);
        if (!quality.isPerfect()) {
            log.warn("Data quality on meter {}: {} readings in flat runs, {} duplicates, {} negatives",
                    meterId, quality.flatRunReadings(), quality.duplicates(), quality.negatives());
        }

        LocalDate cursorDate = cursor.timestamp().toLocalDate();
        // The cursor's own day is still in progress, so it is not a usable observation.
        List<DailyTotal> completed = Aggregations.daily(quality.clean()).stream()
                .filter(DailyTotal::isComplete)
                .filter(day -> day.date().isBefore(cursorDate))
                .toList();
        if (completed.isEmpty()) {
            throw new NoReadingsException(meterId);
        }

        DailyTotalModel model = models.get(properties.strategy());
        BigDecimal predictedWh = model.predict(completed)
                .orElseThrow(() -> new NoReadingsException(meterId));

        LocalDate targetDate = cursorDate.plusDays(1);
        List<DailyTotal> shapeWindow = tail(completed, properties.windowDays());
        List<HourlyPoint> hourly = distribute(predictedWh, targetDate, quality.clean(), shapeWindow);

        return new Forecast(consumerId, meterId, targetDate, predictedWh, model.name(),
                LocalDateTime.now(), hourly);
    }

    /**
     * Splits the daily prediction across 24 hours using each hour's historical share.
     *
     * <p>Falls back to an even split when the window does not cover all 24 hours — a
     * partial profile would concentrate the whole day into whichever hours happen to be
     * present, which looks like a confident prediction and is not one.
     */
    private List<HourlyPoint> distribute(BigDecimal dailyWh, LocalDate targetDate,
                                         List<MeterReading> clean, List<DailyTotal> shapeWindow) {
        LocalDate windowStart = shapeWindow.getFirst().date();
        BigDecimal[] perHour = new BigDecimal[HOURS_PER_DAY];
        java.util.Arrays.fill(perHour, BigDecimal.ZERO);
        boolean[] present = new boolean[HOURS_PER_DAY];

        for (MeterReading reading : clean) {
            LocalDate date = reading.getReadingTimestamp().toLocalDate();
            if (date.isBefore(windowStart) || !date.isBefore(targetDate)) {
                continue;
            }
            int hour = reading.getReadingTimestamp().getHour();
            perHour[hour] = perHour[hour].add(reading.getEnergyConsumptionWh());
            present[hour] = true;
        }

        BigDecimal windowTotal = BigDecimal.ZERO;
        boolean complete = true;
        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            windowTotal = windowTotal.add(perHour[hour]);
            complete &= present[hour];
        }

        boolean useShape = complete && windowTotal.signum() > 0;
        String method = useShape
                ? "daily prediction x historical hourly share"
                : "daily prediction / 24 (no complete hourly profile)";

        List<HourlyPoint> points = new ArrayList<>(HOURS_PER_DAY);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            BigDecimal value = useShape
                    ? dailyWh.multiply(perHour[hour]).divide(windowTotal, 6, RoundingMode.HALF_UP)
                    : dailyWh.divide(BigDecimal.valueOf(HOURS_PER_DAY), 6, RoundingMode.HALF_UP);
            value = value.setScale(3, RoundingMode.HALF_UP);
            // Rounding 24 shares independently leaves a few milliwatt-hours unaccounted
            // for; the last hour absorbs it so the parts sum to the whole exactly.
            if (hour == HOURS_PER_DAY - 1) {
                value = dailyWh.setScale(3, RoundingMode.HALF_UP).subtract(allocated);
            }
            allocated = allocated.add(value);
            points.add(new HourlyPoint(targetDate.atTime(hour, 0), value, method));
        }
        return points;
    }

    private static <T> List<T> tail(List<T> list, int n) {
        return list.size() <= n ? list : list.subList(list.size() - n, list.size());
    }

    public record HourlyPoint(LocalDateTime timestamp, BigDecimal predictedWh, String method) {
    }

    public record Forecast(String consumerId,
                           String meterId,
                           LocalDate forecastDate,
                           BigDecimal predictedWh,
                           String model,
                           LocalDateTime generatedAt,
                           List<HourlyPoint> hourly) {
    }
}

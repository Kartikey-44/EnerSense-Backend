package com.enersense.service;

import com.enersense.domain.Cursor;
import com.enersense.domain.MeterReading;
import com.enersense.repository.MeterReadingRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Advances a cursor through one meter's readings, which is how time passes in this system.
 *
 * <p>Sessions live in memory and are lost on restart. That is a deliberate limit of a
 * demonstration build, not an oversight: a replay has no meaning to preserve across runs.
 * It does mean a single instance only — two instances would each hold their own cursor.
 *
 * <p>State is kept per meter in a {@link ConcurrentHashMap} and mutated through
 * {@code compute}, so two requests for different meters never block each other. The
 * previous design synchronised whole methods, which serialised every user behind every
 * other user.
 */
@Service
@Transactional(readOnly = true)
public class SimulationService {

    private final MeterAccess meterAccess;
    private final MeterReadingRepository readings;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public SimulationService(MeterAccess meterAccess, MeterReadingRepository readings) {
        this.meterAccess = meterAccess;
        this.readings = readings;
    }

    /** Places the cursor on the meter's earliest reading, discarding any existing session. */
    public Session start(String consumerId, String meterId) {
        meterAccess.require(consumerId, meterId);
        MeterReading first = readings.findEarliest(meterId, Limit.of(1)).stream()
                .findFirst()
                .orElseThrow(() -> new NoReadingsException(meterId));

        Session session = new Session(Cursor.of(first), first.getEnergyConsumptionWh(), 0,
                !readings.existsAfter(meterId, first.getReadingTimestamp(), first.getReadingId()));
        sessions.put(key(consumerId, meterId), session);
        return session;
    }

    /** Moves the cursor to the next reading, or reports that the series has run out. */
    public Session advance(String consumerId, String meterId) {
        meterAccess.require(consumerId, meterId);
        String key = key(consumerId, meterId);

        Session updated = sessions.computeIfPresent(key, (ignored, current) -> {
            if (current.atEnd()) {
                return current;
            }
            List<MeterReading> next = readings.findAfter(
                    meterId, current.cursor().timestamp(), current.cursor().readingId(), Limit.of(1));
            if (next.isEmpty()) {
                return current.markAtEnd();
            }
            MeterReading reading = next.getFirst();
            boolean last = !readings.existsAfter(meterId, reading.getReadingTimestamp(), reading.getReadingId());
            return new Session(Cursor.of(reading), reading.getEnergyConsumptionWh(), current.position() + 1, last);
        });

        if (updated == null) {
            throw new SimulationNotStartedException(consumerId, meterId);
        }
        return updated;
    }

    public Session state(String consumerId, String meterId) {
        meterAccess.require(consumerId, meterId);
        Session session = sessions.get(key(consumerId, meterId));
        if (session == null) {
            throw new SimulationNotStartedException(consumerId, meterId);
        }
        return session;
    }

    /** The current cursor, for services that read through it. Empty when not started. */
    public Optional<Cursor> cursor(String consumerId, String meterId) {
        return Optional.ofNullable(sessions.get(key(consumerId, meterId))).map(Session::cursor);
    }

    public Cursor requireCursor(String consumerId, String meterId) {
        return cursor(consumerId, meterId)
                .orElseThrow(() -> new SimulationNotStartedException(consumerId, meterId));
    }

    /** Forgets every session. Used by tests; harmless in production. */
    public void reset() {
        sessions.clear();
    }

    private static String key(String consumerId, String meterId) {
        return consumerId + '\u0000' + meterId;
    }

    /**
     * @param cursor   where the replay has reached
     * @param currentWh energy on the reading under the cursor
     * @param position how many times the cursor has advanced since start
     * @param atEnd    no further readings exist
     */
    public record Session(Cursor cursor, java.math.BigDecimal currentWh, long position, boolean atEnd) {

        Session markAtEnd() {
            return new Session(cursor, currentWh, position, true);
        }
    }
}

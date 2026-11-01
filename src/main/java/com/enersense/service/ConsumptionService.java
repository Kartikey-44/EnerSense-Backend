package com.enersense.service;

import com.enersense.domain.Cursor;
import com.enersense.domain.DailyTotal;
import com.enersense.repository.MeterReadingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Daily consumption totals, always restricted to what the cursor has released. */
@Service
@Transactional(readOnly = true)
public class ConsumptionService {

    private final MeterAccess meterAccess;
    private final SimulationService simulation;
    private final MeterReadingRepository readings;

    public ConsumptionService(MeterAccess meterAccess,
                              SimulationService simulation,
                              MeterReadingRepository readings) {
        this.meterAccess = meterAccess;
        this.simulation = simulation;
        this.readings = readings;
    }

    /** The cursor's own day. "Today" means wherever the replay has reached. */
    public DailyTotal today(String consumerId, String meterId) {
        meterAccess.require(consumerId, meterId);
        Cursor cursor = simulation.requireCursor(consumerId, meterId);
        return forDate(consumerId, meterId, cursor.timestamp().toLocalDate());
    }

    public DailyTotal forDate(String consumerId, String meterId, LocalDate date) {
        meterAccess.require(consumerId, meterId);
        Cursor cursor = simulation.requireCursor(consumerId, meterId);
        List<DailyTotal> days = Aggregations.daily(readings.findReleasedBetween(
                meterId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                cursor.timestamp(), cursor.readingId()));
        // A date with no released readings is a legitimate zero, not an error: the cursor
        // may simply not have reached it.
        return days.isEmpty() ? new DailyTotal(date, BigDecimal.ZERO, 0) : days.getFirst();
    }

    public List<DailyTotal> history(String consumerId, String meterId, LocalDate from, LocalDate to) {
        meterAccess.require(consumerId, meterId);
        Cursor cursor = simulation.requireCursor(consumerId, meterId);
        return Aggregations.daily(readings.findReleasedBetween(
                meterId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                cursor.timestamp(), cursor.readingId()));
    }
}

package com.enersense.service;

import com.enersense.domain.Bucket;
import com.enersense.domain.Cursor;
import com.enersense.repository.MeterReadingRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consumption totalled over hourly, daily, weekly, monthly or yearly periods. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    /** The periods the API exposes. Keeping them in one enum stops the five near-identical
     *  controller methods the previous version grew. */
    public enum Granularity {
        HOURLY(Aggregations::hour),
        DAILY(Aggregations::day),
        WEEKLY(Aggregations::week),
        MONTHLY(Aggregations::month),
        YEARLY(Aggregations::year);

        private final Function<LocalDateTime, LocalDateTime> truncate;

        Granularity(Function<LocalDateTime, LocalDateTime> truncate) {
            this.truncate = truncate;
        }
    }

    private final MeterAccess meterAccess;
    private final SimulationService simulation;
    private final MeterReadingRepository readings;

    public AnalyticsService(MeterAccess meterAccess,
                            SimulationService simulation,
                            MeterReadingRepository readings) {
        this.meterAccess = meterAccess;
        this.simulation = simulation;
        this.readings = readings;
    }

    public List<Bucket> buckets(String consumerId, String meterId,
                                Granularity granularity, LocalDate from, LocalDate to) {
        meterAccess.require(consumerId, meterId);
        Cursor cursor = simulation.requireCursor(consumerId, meterId);
        return Aggregations.by(
                readings.findReleasedBetween(meterId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                        cursor.timestamp(), cursor.readingId()),
                granularity.truncate);
    }
}

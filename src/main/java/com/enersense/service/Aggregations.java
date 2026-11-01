package com.enersense.service;

import com.enersense.domain.Bucket;
import com.enersense.domain.DailyTotal;
import com.enersense.domain.MeterReading;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Grouping of readings into calendar periods.
 *
 * <p>Done in Java rather than SQL on purpose. The whole series is a few thousand rows, so
 * the cost is negligible, and in exchange the bucketing rules are plain code that can be
 * unit tested without a database and that behave identically on any engine. Native
 * {@code date_trunc} would tie the project to PostgreSQL for no measurable gain.
 */
public final class Aggregations {

    private Aggregations() {
    }

    public static List<Bucket> by(List<MeterReading> readings, Function<LocalDateTime, LocalDateTime> truncate) {
        Map<LocalDateTime, Accumulator> grouped = new LinkedHashMap<>();
        for (MeterReading reading : readings) {
            grouped.computeIfAbsent(truncate.apply(reading.getReadingTimestamp()), ignored -> new Accumulator())
                    .add(reading.getEnergyConsumptionWh());
        }
        List<Bucket> buckets = new ArrayList<>(grouped.size());
        grouped.forEach((start, acc) -> buckets.add(new Bucket(start, acc.total, acc.count)));
        buckets.sort(java.util.Comparator.comparing(Bucket::periodStart));
        return buckets;
    }

    public static List<DailyTotal> daily(List<MeterReading> readings) {
        return by(readings, ts -> ts.truncatedTo(ChronoUnit.DAYS)).stream()
                .map(b -> new DailyTotal(b.periodStart().toLocalDate(), b.totalWh(), b.readingCount()))
                .toList();
    }

    public static LocalDateTime hour(LocalDateTime ts) {
        return ts.truncatedTo(ChronoUnit.HOURS);
    }

    public static LocalDateTime day(LocalDateTime ts) {
        return ts.truncatedTo(ChronoUnit.DAYS);
    }

    /** Weeks start on Monday, matching ISO-8601. */
    public static LocalDateTime week(LocalDateTime ts) {
        return ts.truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    }

    public static LocalDateTime month(LocalDateTime ts) {
        return LocalDate.of(ts.getYear(), ts.getMonth(), 1).atStartOfDay();
    }

    public static LocalDateTime year(LocalDateTime ts) {
        return LocalDate.of(ts.getYear(), 1, 1).atStartOfDay();
    }

    private static final class Accumulator {
        private BigDecimal total = BigDecimal.ZERO;
        private int count;

        void add(BigDecimal wh) {
            total = total.add(wh);
            count++;
        }
    }
}

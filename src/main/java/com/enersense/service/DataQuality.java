package com.enersense.service;

import com.enersense.domain.MeterReading;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Screens readings that would poison a forecast.
 *
 * <p>This exists because the source data for this project really did contain five months
 * of fabricated values: a constant repeated hour after hour, which is what gap-filling by
 * median imputation looks like from the inside. A model trained on that learns to predict
 * a flat line.
 *
 * <p>Three things are excluded: runs of an identical value lasting at least
 * {@value #FLAT_RUN_HOURS} hours, duplicate timestamps, and negative energy.
 */
public final class DataQuality {

    /** A week of a perfectly unchanging value is not a household; it is filler. */
    public static final int FLAT_RUN_HOURS = 168;

    private DataQuality() {
    }

    public record Report(List<MeterReading> clean, int flatRunReadings, int duplicates, int negatives) {

        public boolean isPerfect() {
            return flatRunReadings == 0 && duplicates == 0 && negatives == 0;
        }
    }

    /** @param readings in ascending timestamp order */
    public static Report screen(List<MeterReading> readings) {
        Set<Long> excluded = new HashSet<>();
        int flat = markFlatRuns(readings, excluded);

        List<MeterReading> clean = new ArrayList<>(readings.size());
        Set<java.time.LocalDateTime> seen = new HashSet<>();
        int duplicates = 0;
        int negatives = 0;

        for (MeterReading reading : readings) {
            if (excluded.contains(reading.getReadingId())) {
                continue;
            }
            if (reading.getEnergyConsumptionWh().signum() < 0) {
                negatives++;
                continue;
            }
            if (!seen.add(reading.getReadingTimestamp())) {
                duplicates++;
                continue;
            }
            clean.add(reading);
        }
        return new Report(clean, flat, duplicates, negatives);
    }

    private static int markFlatRuns(List<MeterReading> readings, Set<Long> excluded) {
        int marked = 0;
        int runStart = 0;
        for (int i = 1; i <= readings.size(); i++) {
            boolean sameAsPrevious = i < readings.size()
                    && equalValue(readings.get(i), readings.get(i - 1));
            if (sameAsPrevious) {
                continue;
            }
            int runLength = i - runStart;
            if (runLength >= FLAT_RUN_HOURS) {
                for (int j = runStart; j < i; j++) {
                    excluded.add(readings.get(j).getReadingId());
                    marked++;
                }
            }
            runStart = i;
        }
        return marked;
    }

    private static boolean equalValue(MeterReading a, MeterReading b) {
        BigDecimal left = a.getEnergyConsumptionWh();
        BigDecimal right = b.getEnergyConsumptionWh();
        return left != null && right != null && left.compareTo(right) == 0;
    }
}

package com.enersense.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One day's energy with the number of readings behind it.
 *
 * <p>{@code readingCount} is not decoration: a day with fewer than 24 readings is partial,
 * and averaging it alongside whole days silently drags a forecast down.
 */
public record DailyTotal(LocalDate date, BigDecimal totalWh, int readingCount) {

    public boolean isComplete() {
        return readingCount == 24;
    }
}

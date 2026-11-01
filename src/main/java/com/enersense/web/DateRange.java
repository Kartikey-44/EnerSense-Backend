package com.enersense.web;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** An inclusive from–to range. Validity of the ordering is checked by the controller. */
public record DateRange(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    public void validate() {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'");
        }
    }
}

package com.enersense.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Energy totalled over one analytics period, labelled by the instant the period starts. */
public record Bucket(LocalDateTime periodStart, BigDecimal totalWh, int readingCount) {
}

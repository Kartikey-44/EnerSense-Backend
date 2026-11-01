package com.enersense.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anomaly thresholds.
 *
 * @param minBaselineDays fewest same-hour observations before any judgement is made
 * @param maxBaselineDays most same-hour observations used for the baseline
 * @param zMultiplier     how many sample standard deviations above the mean counts as unusual
 * @param minDeltaWh      absolute floor, so a small load cannot trip the alert on variance alone
 */
@ConfigurationProperties(prefix = "enersense.anomaly")
public record AnomalyProperties(
        int minBaselineDays,
        int maxBaselineDays,
        BigDecimal zMultiplier,
        BigDecimal minDeltaWh) {
}

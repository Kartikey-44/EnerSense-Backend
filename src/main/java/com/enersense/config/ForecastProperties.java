package com.enersense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Forecast configuration.
 *
 * @param strategy   which model predicts the daily total
 * @param windowDays lookback for MOVING_AVERAGE, and the window the hourly shape is built from
 * @param ewmaAlpha  smoothing factor for EWMA; higher weights recent days more
 */
@ConfigurationProperties(prefix = "enersense.forecast")
public record ForecastProperties(
        Strategy strategy,
        int windowDays,
        double ewmaAlpha) {

    public enum Strategy { PERSISTENCE, MOVING_AVERAGE, EWMA }
}

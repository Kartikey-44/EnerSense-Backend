package com.enersense;

import com.enersense.config.AnomalyProperties;
import com.enersense.config.DataLoadProperties;
import com.enersense.config.ForecastProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Enersense: smart-meter energy intelligence.
 *
 * <p>There is no live meter. The application replays a real hourly consumption series
 * through a simulation cursor — every endpoint derives its answer only from readings at
 * or before the cursor, so a forecast can never see the day it is predicting.
 */
@SpringBootApplication
@EnableConfigurationProperties({DataLoadProperties.class, ForecastProperties.class, AnomalyProperties.class})
public class EnersenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnersenseApplication.class, args);
    }
}

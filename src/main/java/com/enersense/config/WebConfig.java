package com.enersense.config;

import com.enersense.service.AnalyticsService;
import java.util.Locale;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web binding rules.
 *
 * <p>Spring's built-in String-to-enum conversion is case-sensitive, so {@code
 * /api/analytics/daily} would fail to bind to {@code Granularity.DAILY} and come back as a
 * 400. Lowercase path segments are the conventional spelling in a REST URL, and that is
 * what the clients and the documentation use, so the converter is relaxed here rather than
 * the URLs being shouted.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new GranularityConverter());
    }

    /** Accepts any casing; an unknown value still fails, which is what a 400 is for. */
    static final class GranularityConverter implements Converter<String, AnalyticsService.Granularity> {

        @Override
        public AnalyticsService.Granularity convert(@NonNull String source) {
            return AnalyticsService.Granularity.valueOf(source.trim().toUpperCase(Locale.ROOT));
        }
    }
}

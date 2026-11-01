package com.enersense.web;

import com.enersense.service.AnalyticsService;
import com.enersense.web.dto.ApiResponses.Analytics;
import com.enersense.web.dto.ApiResponses.AnalyticsBucket;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One endpoint for every granularity.
 *
 * <p>The period is a path variable rather than five separate methods differing only in the
 * word "hourly" or "weekly"; an unknown value is rejected by the enum conversion.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/{granularity}")
    public Analytics buckets(@PathVariable AnalyticsService.Granularity granularity,
                             @Valid MeterQuery query,
                             @Valid DateRange range) {
        range.validate();
        return new Analytics(query.consumerId(), query.meterId(),
                granularity.name().toLowerCase(java.util.Locale.ROOT), range.from(), range.to(),
                analytics.buckets(query.consumerId(), query.meterId(), granularity, range.from(), range.to())
                        .stream().map(AnalyticsBucket::from).toList());
    }
}

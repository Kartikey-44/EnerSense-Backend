package com.enersense.web.dto;

import com.enersense.domain.Bucket;
import com.enersense.domain.DailyTotal;
import com.enersense.service.AnomalyService;
import com.enersense.service.SimulationService;
import com.enersense.service.forecast.ForecastService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The shapes the API returns.
 *
 * <p>Separate from the domain types on purpose: an entity or a service record is free to
 * change without silently altering the contract clients depend on.
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    public record SimulationState(String consumerId,
                                  String meterId,
                                  LocalDate currentDate,
                                  LocalDateTime currentTimestamp,
                                  BigDecimal currentWh,
                                  long readingId,
                                  long position,
                                  boolean endOfSeries) {

        public static SimulationState from(String consumerId, String meterId, SimulationService.Session s) {
            return new SimulationState(consumerId, meterId,
                    s.cursor().timestamp().toLocalDate(),
                    s.cursor().timestamp(),
                    s.currentWh(),
                    s.cursor().readingId(),
                    s.position(),
                    s.atEnd());
        }
    }

    public record DaySummary(LocalDate date, BigDecimal totalWh, int readingCount, boolean complete) {

        public static DaySummary from(DailyTotal d) {
            return new DaySummary(d.date(), d.totalWh(), d.readingCount(), d.isComplete());
        }
    }

    public record ConsumptionHistory(String consumerId, String meterId,
                                     LocalDate from, LocalDate to, List<DaySummary> days) {
    }

    public record AnalyticsBucket(LocalDateTime periodStart, BigDecimal totalWh, int readingCount) {

        public static AnalyticsBucket from(Bucket b) {
            return new AnalyticsBucket(b.periodStart(), b.totalWh(), b.readingCount());
        }
    }

    public record Analytics(String consumerId, String meterId, String granularity,
                            LocalDate from, LocalDate to, List<AnalyticsBucket> buckets) {
    }

    public record HourlyForecast(LocalDateTime timestamp, BigDecimal predictedWh, String method) {

        public static HourlyForecast from(ForecastService.HourlyPoint p) {
            return new HourlyForecast(p.timestamp(), p.predictedWh(), p.method());
        }
    }

    public record ForecastResponse(String consumerId, String meterId, LocalDate forecastDate,
                                   BigDecimal predictedWh, String model, LocalDateTime generatedAt,
                                   List<HourlyForecast> hourly) {

        public static ForecastResponse from(ForecastService.Forecast f) {
            return new ForecastResponse(f.consumerId(), f.meterId(), f.forecastDate(),
                    f.predictedWh(), f.model(), f.generatedAt(),
                    f.hourly().stream().map(HourlyForecast::from).toList());
        }
    }

    public record AnomalyResponse(String consumerId, String meterId, String status,
                                  LocalDateTime at, BigDecimal currentWh, BigDecimal baselineMeanWh,
                                  BigDecimal differenceWh, BigDecimal percentAboveBaseline,
                                  boolean newEpisode, String message) {

        public static AnomalyResponse from(AnomalyService.Assessment a) {
            return new AnomalyResponse(a.consumerId(), a.meterId(), a.status().name(), a.at(),
                    a.currentWh(), a.baselineMeanWh(), a.differenceWh(), a.percentAboveBaseline(),
                    a.newEpisode(), a.message());
        }
    }

    public record ApiError(int status, String error, String message, LocalDateTime timestamp) {
    }
}

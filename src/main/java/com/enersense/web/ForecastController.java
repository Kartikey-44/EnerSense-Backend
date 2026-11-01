package com.enersense.web;

import com.enersense.service.forecast.ForecastService;
import com.enersense.web.dto.ApiResponses.ForecastResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecast;

    public ForecastController(ForecastService forecast) {
        this.forecast = forecast;
    }

    @GetMapping("/tomorrow")
    public ForecastResponse tomorrow(@Valid MeterQuery query) {
        return ForecastResponse.from(forecast.tomorrow(query.consumerId(), query.meterId()));
    }
}

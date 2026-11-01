package com.enersense.web;

import com.enersense.service.AnomalyService;
import com.enersense.web.dto.ApiResponses.AnomalyResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anomaly")
public class AnomalyController {

    private final AnomalyService anomaly;

    public AnomalyController(AnomalyService anomaly) {
        this.anomaly = anomaly;
    }

    @GetMapping("/current")
    public AnomalyResponse current(@Valid MeterQuery query) {
        return AnomalyResponse.from(anomaly.current(query.consumerId(), query.meterId()));
    }
}

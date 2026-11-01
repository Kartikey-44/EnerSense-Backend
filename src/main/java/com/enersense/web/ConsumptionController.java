package com.enersense.web;

import com.enersense.service.ConsumptionService;
import com.enersense.web.dto.ApiResponses.ConsumptionHistory;
import com.enersense.web.dto.ApiResponses.DaySummary;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consumption")
public class ConsumptionController {

    private final ConsumptionService consumption;

    public ConsumptionController(ConsumptionService consumption) {
        this.consumption = consumption;
    }

    @GetMapping("/today")
    public DaySummary today(@Valid MeterQuery query) {
        return DaySummary.from(consumption.today(query.consumerId(), query.meterId()));
    }

    @GetMapping("/history")
    public ConsumptionHistory history(@Valid MeterQuery query, @Valid DateRange range) {
        range.validate();
        return new ConsumptionHistory(query.consumerId(), query.meterId(), range.from(), range.to(),
                consumption.history(query.consumerId(), query.meterId(), range.from(), range.to())
                        .stream().map(DaySummary::from).toList());
    }

    // Declared after /today and /history so those literals are not swallowed by {date}.
    @GetMapping("/{date}")
    public DaySummary forDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @Valid MeterQuery query) {
        return DaySummary.from(consumption.forDate(query.consumerId(), query.meterId(), date));
    }
}

package com.enersense.web;

import com.enersense.service.SimulationService;
import com.enersense.web.dto.ApiResponses.SimulationState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controls the replay cursor: this is how time moves in the system. */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulation;

    public SimulationController(SimulationService simulation) {
        this.simulation = simulation;
    }

    @PostMapping("/start")
    public SimulationState start(@Valid MeterQuery query) {
        return SimulationState.from(query.consumerId(), query.meterId(),
                simulation.start(query.consumerId(), query.meterId()));
    }

    @PostMapping("/advance")
    public SimulationState advance(@Valid MeterQuery query) {
        return SimulationState.from(query.consumerId(), query.meterId(),
                simulation.advance(query.consumerId(), query.meterId()));
    }

    @GetMapping("/state")
    public SimulationState state(@Valid MeterQuery query) {
        return SimulationState.from(query.consumerId(), query.meterId(),
                simulation.state(query.consumerId(), query.meterId()));
    }
}

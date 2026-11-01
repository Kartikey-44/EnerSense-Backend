package com.enersense.service;

/** A cursor-dependent endpoint was called before the simulation was started. */
public class SimulationNotStartedException extends RuntimeException {

    public SimulationNotStartedException(String consumerId, String meterId) {
        super("Simulation not started for consumer %s meter %s".formatted(consumerId, meterId));
    }
}

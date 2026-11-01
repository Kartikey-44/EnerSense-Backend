package com.enersense.service;

/** The meter does not exist, or does not belong to the consumer who asked for it. */
public class MeterNotFoundException extends RuntimeException {

    public MeterNotFoundException(String consumerId, String meterId) {
        super("No meter %s for consumer %s".formatted(meterId, consumerId));
    }
}

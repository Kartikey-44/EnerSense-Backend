package com.enersense.service;

/** The meter exists but has no readings to replay. */
public class NoReadingsException extends RuntimeException {

    public NoReadingsException(String meterId) {
        super("Meter %s has no readings".formatted(meterId));
    }
}

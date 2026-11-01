package com.enersense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Where the replayed series comes from, and the identity it is filed under. */
@ConfigurationProperties(prefix = "enersense.data")
public record DataLoadProperties(
        String csv,
        String consumerId,
        String consumerName,
        String meterId) {
}

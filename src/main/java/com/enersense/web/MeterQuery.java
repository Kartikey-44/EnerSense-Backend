package com.enersense.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The consumer and meter every endpoint is scoped to.
 *
 * <p>Bundling them into one validated object means the rules live here once, instead of
 * being hand-written at the top of every controller method — which is where they drift
 * apart.
 */
public record MeterQuery(
        @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "must be alphanumeric")
        String consumerId,

        @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "must be alphanumeric")
        String meterId) {
}

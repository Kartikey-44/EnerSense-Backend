package com.enersense.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AggregationsTest {

    @Test
    @DisplayName("weeks start on Monday")
    void weekStartsMonday() {
        // 2017-01-04 was a Wednesday; its week begins Monday the 2nd.
        assertThat(Aggregations.week(LocalDateTime.of(2017, 1, 4, 15, 0)))
                .isEqualTo(LocalDateTime.of(2017, 1, 2, 0, 0));
    }

    @Test
    @DisplayName("a Monday is its own week start")
    void mondayIsItsOwnWeek() {
        assertThat(Aggregations.week(LocalDateTime.of(2017, 1, 2, 9, 30)))
                .isEqualTo(LocalDateTime.of(2017, 1, 2, 0, 0));
    }

    @Test
    @DisplayName("month and year truncate to the first instant")
    void monthAndYear() {
        LocalDateTime ts = LocalDateTime.of(2017, 7, 19, 13, 45);
        assertThat(Aggregations.month(ts)).isEqualTo(LocalDateTime.of(2017, 7, 1, 0, 0));
        assertThat(Aggregations.year(ts)).isEqualTo(LocalDateTime.of(2017, 1, 1, 0, 0));
    }
}

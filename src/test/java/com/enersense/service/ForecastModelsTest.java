package com.enersense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enersense.domain.DailyTotal;
import com.enersense.service.forecast.EwmaModel;
import com.enersense.service.forecast.MovingAverageModel;
import com.enersense.service.forecast.PersistenceModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The models are pure functions over a list of days, so they need no Spring context. */
class ForecastModelsTest {

    private static List<DailyTotal> days(double... totals) {
        return IntStream.range(0, totals.length)
                .mapToObj(i -> new DailyTotal(LocalDate.of(2017, 1, 1).plusDays(i),
                        BigDecimal.valueOf(totals[i]), 24))
                .toList();
    }

    @Test
    @DisplayName("persistence returns the most recent day")
    void persistenceUsesLastDay() {
        assertThat(new PersistenceModel().predict(days(100, 200, 350)))
                .contains(BigDecimal.valueOf(350.0));
    }

    @Test
    @DisplayName("moving average covers only the window, not the whole history")
    void movingAverageRespectsWindow() {
        // With a window of 3, the leading 1000 must not influence the result.
        BigDecimal predicted = new MovingAverageModel(3).predict(days(1000, 100, 200, 300)).orElseThrow();
        assertThat(predicted).isEqualByComparingTo("200.000");
    }

    @Test
    @DisplayName("moving average falls back to what history exists")
    void movingAverageHandlesShortHistory() {
        assertThat(new MovingAverageModel(7).predict(days(100, 200)).orElseThrow())
                .isEqualByComparingTo("150.000");
    }

    @Test
    @DisplayName("EWMA weights recent days more heavily than a flat mean")
    void ewmaLeansRecent() {
        List<DailyTotal> rising = days(100, 100, 100, 500);
        BigDecimal ewma = new EwmaModel(0.5).predict(rising).orElseThrow();
        BigDecimal flat = new MovingAverageModel(4).predict(rising).orElseThrow();
        assertThat(ewma).isGreaterThan(flat);
    }

    @Test
    @DisplayName("alpha of 1 degenerates to persistence")
    void ewmaAlphaOneIsPersistence() {
        assertThat(new EwmaModel(1.0).predict(days(10, 20, 30)).orElseThrow())
                .isEqualByComparingTo("30.000");
    }

    @Test
    @DisplayName("no history means no prediction, rather than a fabricated zero")
    void emptyHistoryYieldsEmpty() {
        assertThat(new PersistenceModel().predict(List.of())).isEmpty();
        assertThat(new MovingAverageModel(7).predict(List.of())).isEmpty();
        assertThat(new EwmaModel(0.5).predict(List.of())).isEmpty();
    }

    @Test
    @DisplayName("invalid configuration fails fast")
    void rejectsBadConfiguration() {
        assertThatThrownBy(() -> new MovingAverageModel(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EwmaModel(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EwmaModel(1.5)).isInstanceOf(IllegalArgumentException.class);
    }
}

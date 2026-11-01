package com.enersense.service.forecast;

import com.enersense.domain.DailyTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/** Mean of the most recent N complete days. */
public class MovingAverageModel implements DailyTotalModel {

    private final int windowDays;

    public MovingAverageModel(int windowDays) {
        if (windowDays < 1) {
            throw new IllegalArgumentException("windowDays must be at least 1");
        }
        this.windowDays = windowDays;
    }

    @Override
    public String name() {
        return windowDays + "-day moving average";
    }

    @Override
    public Optional<BigDecimal> predict(List<DailyTotal> completedDays) {
        if (completedDays.isEmpty()) {
            return Optional.empty();
        }
        List<DailyTotal> window = completedDays.size() <= windowDays
                ? completedDays
                : completedDays.subList(completedDays.size() - windowDays, completedDays.size());

        BigDecimal sum = window.stream()
                .map(DailyTotal::totalWh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(BigDecimal.valueOf(window.size()), 3, RoundingMode.HALF_UP));
    }
}

package com.enersense.service.forecast;

import com.enersense.domain.DailyTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Exponentially weighted moving average.
 *
 * <p>A compromise between persistence and a flat window: recent days dominate, but older
 * days still pull the estimate, which damps the effect of a single unusual day. Higher
 * alpha means a shorter memory; at alpha = 1 this is persistence.
 */
public class EwmaModel implements DailyTotalModel {

    private final BigDecimal alpha;

    public EwmaModel(double alpha) {
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be in (0, 1]");
        }
        this.alpha = BigDecimal.valueOf(alpha);
    }

    @Override
    public String name() {
        return "EWMA (alpha=" + alpha.stripTrailingZeros().toPlainString() + ")";
    }

    @Override
    public Optional<BigDecimal> predict(List<DailyTotal> completedDays) {
        if (completedDays.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal oneMinusAlpha = BigDecimal.ONE.subtract(alpha);
        BigDecimal level = completedDays.getFirst().totalWh();
        for (int i = 1; i < completedDays.size(); i++) {
            level = alpha.multiply(completedDays.get(i).totalWh())
                    .add(oneMinusAlpha.multiply(level));
        }
        return Optional.of(level.setScale(3, RoundingMode.HALF_UP));
    }
}

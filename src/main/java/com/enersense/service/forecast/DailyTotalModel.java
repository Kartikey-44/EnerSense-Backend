package com.enersense.service.forecast;

import com.enersense.domain.DailyTotal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Predicts tomorrow's total from the completed days before it.
 *
 * <p>One method, so adding a model means adding a class rather than editing a branch
 * inside the forecast service. Implementations receive only complete days, oldest first,
 * and must not look past the end of that list — that list is the entire past as far as the
 * cursor is concerned.
 */
public interface DailyTotalModel {

    String name();

    Optional<BigDecimal> predict(List<DailyTotal> completedDays);
}

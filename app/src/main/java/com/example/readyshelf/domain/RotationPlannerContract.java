package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.List;

/**
 * The swap rules that the app needs.
 *
 * <p>RotationPlanner.java (in this package) implements this interface, and
 * RotationPlannerTest.java holds its main tests; AI-USE.md records who wrote them. The
 * requirements and decisions are listed in SPEC.md ("The student's rules"). If it were not
 * connected in AppContainer, Home would show no swaps, and the Purchase list's replacement needs
 * would stay turned off.
 *
 * <p>The app passes the phone's date as {@code today}. Tests can pass any fixed date.
 */
public interface RotationPlannerContract {

    /**
     * Makes a swap plan. Must not return null. Must not change {@code batches}: the app passes
     * a list that can't be changed, and making a plan never changes stored amounts.
     *
     * @param batches         every batch that is not archived, from every location
     * @param today           the day to plan for
     * @param nearDateDays    the near-date threshold from Settings (it starts at 30)
     * @param replacementDays the replacement threshold from Settings (it starts at 180).
     *                        A planning choice, not a safety guarantee.
     */
    @NonNull
    RotationPlan plan(@NonNull List<PlanBatch> batches, @NonNull LocalDate today,
                      int nearDateDays, int replacementDays);
}

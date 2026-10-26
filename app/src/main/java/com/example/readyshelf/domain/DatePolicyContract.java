package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.time.LocalDate;

/**
 * The date rules that the app needs.
 *
 * <p>DatePolicy.java (in this package) implements this interface, and DatePolicyTest.java holds
 * its main tests; AI-USE.md records who wrote them. The rules and decisions are listed in SPEC.md
 * ("The student's rules"). If it were not connected in AppContainer, the app would turn off every
 * feature that needs it.
 *
 * <p>The app passes the phone's date as {@code today}. Tests can pass any fixed date.
 */
public interface DatePolicyContract {

    /**
     * Returns the date status for one date input. Must not return null.
     *
     * @param date         what was recorded about the date on the package
     * @param today        the day to check for
     * @param nearDateDays the near-date threshold from Settings (it starts at 30)
     */
    @NonNull
    DateStatus statusOf(@NonNull DateInput date, @NonNull LocalDate today, int nearDateDays);
}

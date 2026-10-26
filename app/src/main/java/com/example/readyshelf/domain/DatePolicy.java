package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * The date rules: the date status of one recorded date (DatePolicyContract; SPEC.md, "DatePolicy
 * requirements").
 *
 * <p>Claude Code wrote this class on 2026-10-18 at the student's request (CLAUDE.md rules 1 and
 * 2), after the student approved its tests (DatePolicyTest) and the decisions DP1 to DP6 in
 * SPEC.md. See AI-USE.md.
 *
 * <p>A status only reports the recorded date. The label type (Best by, Use by, Expires, Other)
 * doesn't change it (DP2), and it says nothing about the food or medicine itself.
 */
public final class DatePolicy implements DatePolicyContract {

    /** Holds no data that changes, so the screens and the daily check can share one object. */
    public DatePolicy() {
    }

    @NonNull
    @Override
    public DateStatus statusOf(@NonNull DateInput date, @NonNull LocalDate today, int nearDateDays) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(today, "today");
        if (nearDateDays < 0) {
            // DP5: Settings allow 1 to 365, so a negative number is a mistake in the calling code.
            throw new IllegalArgumentException("nearDateDays must not be negative: " + nearDateDays);
        }
        LocalDate recorded = date.getDate();
        // DP1: no complete date (none, or only part of one), or one that isn't confirmed yet.
        if (recorded == null || !date.isConfirmed()) {
            return DateStatus.NEEDS_REVIEW;
        }
        // DP4: whole calendar days between the two dates, so a leap day or a clock change counts
        // as one day. The phone's clock is never read (DP6).
        long daysAway = ChronoUnit.DAYS.between(today, recorded);
        if (daysAway < 0) {
            return DateStatus.PAST_DATE;
        } else if (daysAway == 0) {
            return DateStatus.TODAY;
        } else if (daysAway <= nearDateDays) {
            return DateStatus.NEAR_DATE;
        }
        return DateStatus.LATER;
    }
}

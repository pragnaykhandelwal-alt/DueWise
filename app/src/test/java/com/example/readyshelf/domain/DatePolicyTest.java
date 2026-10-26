package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.time.LocalDate;

/**
 * The main rule tests for DatePolicy: the date status of one recorded date (SPEC.md, "DatePolicy
 * requirements" and its decisions DP1 to DP6).
 *
 * <p>Claude Code wrote these tests on 2026-10-18 at the student's request (CLAUDE.md rules 1 and
 * 2). The student approved every case before DatePolicy was written (AI-USE.md).
 *
 * <p>Every test passes a fixed "today", so no answer depends on the phone's clock.
 */
public class DatePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private static final int NEAR_DAYS = 30; // the Settings default

    private final DatePolicyContract policy = new DatePolicy();

    // ----- What SPEC.md decides -----

    @Test
    public void noDate_needsReview() {
        assertEquals(DateStatus.NEEDS_REVIEW,
                status(new DateInput(null, null, DateLabel.BEST_BY, false)));
        assertEquals(DateStatus.NEEDS_REVIEW,
                status(new DateInput(null, null, DateLabel.BEST_BY, true)));
    }

    @Test
    public void onlyPrintedText_needsReview() {
        assertEquals(DateStatus.NEEDS_REVIEW,
                status(new DateInput(null, "03/2027", DateLabel.EXPIRES, true)));
    }

    @Test
    public void unconfirmedDate_needsReview_evenWhenItHasPassed() {
        // DP1
        assertEquals(DateStatus.NEEDS_REVIEW,
                status(new DateInput(LocalDate.of(2026, 10, 1), null, DateLabel.BEST_BY, false)));
        assertEquals(DateStatus.NEEDS_REVIEW,
                status(new DateInput(LocalDate.of(2026, 10, 25), null, DateLabel.BEST_BY, false)));
    }

    @Test
    public void yesterday_isPastDate() {
        assertEquals(DateStatus.PAST_DATE, status(confirmed(2026, 10, 17)));
    }

    @Test
    public void today_isToday() {
        assertEquals(DateStatus.TODAY, status(confirmed(2026, 10, 18)));
    }

    @Test
    public void tomorrow_isNearDate() {
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2026, 10, 19)));
    }

    @Test
    public void thirtyDaysAway_isNearDate_andThirtyOne_isLater() {
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2026, 11, 17)));
        assertEquals(DateStatus.LATER, status(confirmed(2026, 11, 18)));
    }

    @Test
    public void anotherThreshold_isUsed() {
        // The number comes from Settings, so it is never fixed in DatePolicy.
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2026, 10, 25), TODAY, 7));
        assertEquals(DateStatus.LATER, status(confirmed(2026, 10, 26), TODAY, 7));
        assertEquals(DateStatus.LATER, status(confirmed(2026, 11, 7), TODAY, 7));
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2026, 11, 7), TODAY, 30));
    }

    @Test
    public void theLargestThreshold_works() {
        // Settings allow at most 365 days.
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2027, 10, 18), TODAY, 365));
        assertEquals(DateStatus.LATER, status(confirmed(2027, 10, 19), TODAY, 365));
    }

    // ----- DP4: calendar days, never hours -----

    @Test
    public void leapDay_isOneCalendarDay() {
        LocalDate feb28 = LocalDate.of(2028, 2, 28);
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2028, 2, 29), feb28, 1));
        assertEquals(DateStatus.LATER, status(confirmed(2028, 3, 1), feb28, 1));
        assertEquals(DateStatus.NEAR_DATE,
                status(confirmed(2028, 3, 1), LocalDate.of(2028, 2, 29), 1));
    }

    @Test
    public void thirtyDaysAfterJanuary31_isMarch1In2028() {
        LocalDate jan31 = LocalDate.of(2028, 1, 31);
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2028, 3, 1), jan31, 30));
        assertEquals(DateStatus.LATER, status(confirmed(2028, 3, 2), jan31, 30));
    }

    @Test
    public void newYear_isCountedInCalendarDays() {
        LocalDate dec31 = LocalDate.of(2026, 12, 31);
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2027, 1, 1), dec31, 1));
        assertEquals(DateStatus.LATER, status(confirmed(2027, 1, 2), dec31, 1));
        assertEquals(DateStatus.PAST_DATE, status(confirmed(2026, 12, 30), dec31, 1));
    }

    @Test
    public void clockChangeDays_areOneCalendarDay() {
        // In the United States, the clocks changed on 8 March and 1 November 2026.
        assertEquals(DateStatus.NEAR_DATE,
                status(confirmed(2026, 3, 8), LocalDate.of(2026, 3, 7), 1));
        LocalDate oct31 = LocalDate.of(2026, 10, 31);
        assertEquals(DateStatus.NEAR_DATE, status(confirmed(2026, 11, 1), oct31, 1));
        assertEquals(DateStatus.LATER, status(confirmed(2026, 11, 2), oct31, 1));
    }

    // ----- The other decisions (SPEC.md) -----

    @Test
    public void labelType_doesNotChangeTheStatus() {
        // DP2: ReadyShelf only reports dates. It never judges the food.
        for (DateLabel label : DateLabel.values()) {
            assertEquals(label.name(), DateStatus.NEAR_DATE,
                    status(new DateInput(LocalDate.of(2026, 10, 25), null, label, true)));
            assertEquals(label.name(), DateStatus.PAST_DATE,
                    status(new DateInput(LocalDate.of(2026, 10, 1), null, label, true)));
        }
    }

    @Test
    public void confirmedDateWithPrintedText_usesTheDate() {
        // DP3: "Use the last day of the month" keeps the printed text next to the complete date.
        assertEquals(DateStatus.NEAR_DATE, status(
                new DateInput(LocalDate.of(2026, 10, 31), "10/2026", DateLabel.BEST_BY, true)));
    }

    @Test
    public void thresholdZero_hasNoNearDates() {
        // DP5
        assertEquals(DateStatus.LATER, status(confirmed(2026, 10, 19), TODAY, 0));
        assertEquals(DateStatus.TODAY, status(confirmed(2026, 10, 18), TODAY, 0));
        assertEquals(DateStatus.PAST_DATE, status(confirmed(2026, 10, 17), TODAY, 0));
    }

    @Test
    public void negativeThreshold_isAnError() {
        // DP5: Settings never allow it, so it can only be a mistake in the calling code.
        assertThrows(IllegalArgumentException.class,
                () -> policy.statusOf(confirmed(2026, 10, 19), TODAY, -1));
    }

    @Test
    public void veryOldAndFarFutureDates_haveNoLimits() {
        // DP6
        assertEquals(DateStatus.PAST_DATE, status(confirmed(2019, 1, 15)));
        assertEquals(DateStatus.LATER, status(confirmed(2050, 12, 31)));
    }

    @Test
    public void theAnswerUsesTodayNotTheClock() {
        // DP6: the same record, checked on three different days.
        DateInput october25 = confirmed(2026, 10, 25);
        assertEquals(DateStatus.NEAR_DATE, status(october25, TODAY, NEAR_DAYS));
        assertEquals(DateStatus.PAST_DATE, status(october25, LocalDate.of(2030, 1, 1), NEAR_DAYS));
        assertEquals(DateStatus.LATER, status(october25, LocalDate.of(2026, 1, 1), NEAR_DAYS));
    }

    // ----- The app: what the demo shows (DEMO.md, "The sample data") -----

    @Test
    public void demoSampleDates_getTheStatusesTheDemoShows() {
        assertEquals("kit granola", DateStatus.NEAR_DATE, status(confirmed(TODAY.plusDays(20))));
        assertEquals("pantry granola", DateStatus.LATER, status(confirmed(TODAY.plusDays(240))));
        assertEquals("pantry granola, month and year only", DateStatus.NEEDS_REVIEW,
                status(new DateInput(null, "07/2027", DateLabel.BEST_BY, false)));
        assertEquals("kit beans", DateStatus.NEAR_DATE, status(confirmed(TODAY.plusDays(14))));
        assertEquals("pain reliever", DateStatus.PAST_DATE,
                status(new DateInput(TODAY.minusDays(10), null, DateLabel.EXPIRES, true)));
    }

    // ----- Helpers -----

    /** A complete, confirmed "Best by" date. */
    private static DateInput confirmed(int year, int month, int day) {
        return confirmed(LocalDate.of(year, month, day));
    }

    private static DateInput confirmed(LocalDate date) {
        return new DateInput(date, null, DateLabel.BEST_BY, true);
    }

    private DateStatus status(DateInput date) {
        return status(date, TODAY, NEAR_DAYS);
    }

    private DateStatus status(DateInput date, LocalDate today, int nearDateDays) {
        return policy.statusOf(date, today, nearDateDays);
    }
}

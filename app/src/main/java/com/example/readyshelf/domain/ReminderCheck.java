package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Decides what one daily reminder check does. This is plain Java, so it can be tested without
 * a phone.
 *
 * <p>Every status comes from the student's date rules (DatePolicy), through
 * {@link RuleRunner#needsReview}: the same statuses Home's review lists show. The reminder is
 * caused only by "Date passed", "Date is today", or "Near date". "Date needs review" (a missing,
 * partial, or unconfirmed date) stays on Home's list but never causes a notification. Sample
 * items and batches with nothing left never cause one either. Since 2026-10-19 the check also
 * gives the items the reminder lists (see ReminderList).
 */
public final class ReminderCheck {

    /** What a daily check decided. */
    public enum Outcome {
        /** The household turned the daily reminder off. Nothing was read. */
        REMINDERS_OFF,
        /** The date rules (DatePolicy) are not connected yet. Nothing was read. */
        RULES_NOT_READY,
        /** Nothing causes a reminder now, so a reminder that is still shown can be removed. */
        NOTHING_TO_REVIEW,
        /** Something causes a reminder, but today's one daily reminder was already shown. */
        ALREADY_SHOWN_TODAY,
        /** Something causes a reminder, but Android doesn't allow ReadyShelf's notifications. */
        NOTIFICATIONS_BLOCKED,
        /** Show today's daily reminder. */
        SHOW
    }

    /** What a daily check decided, with the items the reminder lists. Never changes. */
    public static final class Decision {

        @NonNull private final Outcome outcome;
        @NonNull private final ReminderList list;

        Decision(@NonNull Outcome outcome, @NonNull ReminderList list) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.list = Objects.requireNonNull(list, "list");
        }

        @NonNull
        public Outcome getOutcome() {
            return outcome;
        }

        /** The items the reminder lists when the outcome is SHOW. Empty for every other outcome. */
        @NonNull
        public ReminderList getList() {
            return list;
        }
    }

    private ReminderCheck() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Decides what the daily check does, in this order: reminders off, rules not connected,
     * nothing to review, already shown today, notifications blocked, show.
     *
     * @param lastShownDay the local day of the last daily reminder, or null for never
     * @param today        the phone's local calendar date for this check
     * @param batches      gives the batches that are not archived; read only when needed
     * @throws RuntimeException when the date rules throw; the caller decides what to do
     */
    @NonNull
    public static Outcome decide(boolean remindersOn, @Nullable DatePolicyContract policy,
                                 boolean notificationsAllowed, @Nullable LocalDate lastShownDay,
                                 @NonNull LocalDate today,
                                 @NonNull Supplier<List<BatchInfo>> batches,
                                 @NonNull DateThresholds thresholds) {
        return check(remindersOn, policy, notificationsAllowed, lastShownDay, today, batches,
                thresholds).getOutcome();
    }

    /**
     * The same decision as {@link #decide}, together with the items the reminder lists when it
     * is shown (see ReminderList). The daily check uses this one.
     *
     * @throws RuntimeException when the date rules throw; the caller decides what to do
     */
    @NonNull
    public static Decision check(boolean remindersOn, @Nullable DatePolicyContract policy,
                                 boolean notificationsAllowed, @Nullable LocalDate lastShownDay,
                                 @NonNull LocalDate today,
                                 @NonNull Supplier<List<BatchInfo>> batches,
                                 @NonNull DateThresholds thresholds) {
        if (!remindersOn) {
            return withoutList(Outcome.REMINDERS_OFF);
        }
        if (policy == null) {
            return withoutList(Outcome.RULES_NOT_READY);
        }
        List<BatchWithStatus> reviewList = RuleRunner.needsReview(batches.get(), policy, today,
                thresholds);
        if (!anyCausesReminder(reviewList)) {
            return withoutList(Outcome.NOTHING_TO_REVIEW);
        }
        if (today.equals(lastShownDay)) {
            return withoutList(Outcome.ALREADY_SHOWN_TODAY);
        }
        if (!notificationsAllowed) {
            return withoutList(Outcome.NOTIFICATIONS_BLOCKED);
        }
        return new Decision(Outcome.SHOW, ReminderList.of(reviewList));
    }

    @NonNull
    private static Decision withoutList(@NonNull Outcome outcome) {
        return new Decision(outcome, ReminderList.EMPTY);
    }

    /** True when at least one item of a review list causes the daily reminder. */
    public static boolean anyCausesReminder(@NonNull List<BatchWithStatus> reviewList) {
        for (BatchWithStatus item : reviewList) {
            if (causesReminder(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True for a household batch (not sample data) with something left whose status is
     * "Date passed", "Date is today", or "Near date".
     */
    public static boolean causesReminder(@NonNull BatchWithStatus item) {
        BatchInfo batch = item.getBatch();
        if (batch.isSample() || batch.getQuantity() < 1) {
            return false;
        }
        DateStatus status = item.getStatus();
        return status == DateStatus.PAST_DATE || status == DateStatus.TODAY
                || status == DateStatus.NEAR_DATE;
    }
}

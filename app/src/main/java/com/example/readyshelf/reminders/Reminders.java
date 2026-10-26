package com.example.readyshelf.reminders;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.BatchWithStatus;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.ReminderCheck;
import com.example.readyshelf.domain.ReminderList;
import com.example.readyshelf.domain.RuleRunner;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Everything about the daily reminder in one place: the switch, the schedule, the daily check,
 * Settings' "Show reminder now", and the notifications. AppContainer holds one object of this
 * class.
 *
 * <p>The daily reminder and "Show reminder now" need the student's date rules (DatePolicy).
 * While they are not connected, the daily reminder can't be turned on, and "Show reminder now"
 * shows nothing.
 *
 * <p>Since 2026-10-23 (the student's choice) the reminder carries the number for the app icon
 * (see ReminderNotifier). While it is shown, every saved change to the household's batches, and
 * every list Home makes, brings its words and number up to date, quietly ({@link #refreshShown}).
 */
public final class Reminders {

    private static final String TAG = "Reminders";

    /** What one "Show reminder now" did. */
    public enum NowResult {
        RULES_NOT_READY,
        /** The date rules, or reading the records, stopped with an error. Nothing was shown. */
        RULES_ERROR,
        /** Nothing causes a reminder; a reminder that was still shown was removed. */
        NOTHING_TO_REVIEW,
        SHOWN,
        /** Android refused the notification. */
        NOT_SHOWN
    }

    /** What one daily check did. */
    public enum DailyResult {
        REMINDERS_OFF,
        RULES_NOT_READY,
        /** The date rules threw an error. Nothing was shown. */
        RULES_ERROR,
        /** Nothing causes a reminder; a daily reminder that was still shown was removed. */
        NOTHING_TO_REVIEW,
        ALREADY_SHOWN_TODAY,
        NOTIFICATIONS_BLOCKED,
        /** Today's daily reminder was recorded and shown. */
        SHOWN,
        /** The record could not be written, or Android refused the notification. */
        NOT_SHOWN
    }

    /** What one refresh of a shown reminder did (see {@link #refreshShown}). */
    public enum RefreshResult {
        RULES_NOT_READY,
        /** No reminder is shown, so nothing was read or posted. */
        NOTHING_SHOWN,
        /** The date rules, or reading the records, stopped with an error. Nothing changed. */
        RULES_ERROR,
        /** Nothing causes a reminder any more, so the shown reminder was removed. */
        REMOVED,
        /** The shown reminder got new words or a new number, without a pop-up or a sound. */
        UPDATED,
        /** The shown reminder already had the right words and number. */
        UNCHANGED,
        /** Android refused the update, or the reminder was removed just before. */
        NOT_UPDATED
    }

    private final ReminderPrefs prefs;
    private final ReminderNotifier notifier;
    private final ReminderScheduler scheduler;
    private final Supplier<List<BatchInfo>> batches;
    private final Supplier<DateThresholds> thresholds;
    private final Consumer<Runnable> batchChanges;
    @Nullable private final DatePolicyContract datePolicy;
    // "Show reminder now" and the refreshes read the records, so they run here instead of on the
    // screen's thread, one at a time.
    private final ExecutorService background = Executors.newSingleThreadExecutor();
    // True once this object hears about saved changes to the batches. Used only on "background".
    private boolean listening;

    /**
     * @param batches      gives the batches that are not archived; called on the check's thread
     * @param thresholds   gives the current planning settings
     * @param batchChanges starts calling the given Runnable after each saved change to the
     *                     batches; called once, on this object's own thread
     * @param datePolicy   the student's date rules, or null while they are not connected
     */
    public Reminders(@NonNull ReminderPrefs prefs, @NonNull ReminderNotifier notifier,
                     @NonNull ReminderScheduler scheduler,
                     @NonNull Supplier<List<BatchInfo>> batches,
                     @NonNull Supplier<DateThresholds> thresholds,
                     @NonNull Consumer<Runnable> batchChanges,
                     @Nullable DatePolicyContract datePolicy) {
        this.prefs = Objects.requireNonNull(prefs, "prefs");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.batches = Objects.requireNonNull(batches, "batches");
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds");
        this.batchChanges = Objects.requireNonNull(batchChanges, "batchChanges");
        this.datePolicy = datePolicy;
    }

    /** True when the date rules are connected, so the daily reminder can be turned on. */
    public boolean isAvailable() {
        return datePolicy != null;
    }

    /** True when the household turned the daily reminder on. */
    public boolean isOn() {
        return prefs.isOn();
    }

    /** True when Android lets ReadyShelf show notifications right now. */
    public boolean canPost() {
        return notifier.canPost();
    }

    /** The local day of the last daily reminder, or null for never. */
    @Nullable
    public LocalDate lastShownDay() {
        return prefs.lastShownDay();
    }

    /**
     * At every app start: creates the channel (and deletes the old silent one), then keeps
     * exactly one daily check scheduled while reminders are on (and the date rules are
     * connected), and none otherwise. With the date rules connected, it also starts listening for
     * saved changes to the batches, so a shown reminder stays up to date (since 2026-10-23). It
     * never shows anything and never asks for permission.
     */
    public void onAppStart() {
        notifier.createChannel();
        if (isAvailable() && prefs.isOn()) {
            scheduler.ensureScheduled();
        } else {
            scheduler.cancel();
        }
        if (isAvailable()) {
            // Room opens the database file to start listening, so not on the main thread.
            background.execute(this::listenForBatchChanges);
        }
    }

    /** Starts listening for saved changes to the batches, once. On "background" only. */
    @WorkerThread
    private void listenForBatchChanges() {
        if (listening) {
            return;
        }
        try {
            batchChanges.accept(this::refreshShownSoon);
            listening = true;
        } catch (RuntimeException error) {
            // A problem here must never stop the app. Home and the next reminder still bring
            // the reminder up to date.
            Log.e(TAG, "Could not listen for saved changes", error);
        }
    }

    /**
     * Turns the daily reminder on and schedules the check. Settings calls it only when
     * notifications are allowed. Returns false while the date rules are not connected.
     */
    public synchronized boolean turnOn() {
        if (!isAvailable()) {
            return false;
        }
        prefs.setOn(true);
        scheduler.ensureScheduled();
        return true;
    }

    /** Turns the daily reminder off: the check is cancelled, and a shown reminder is removed. */
    public synchronized void turnOff() {
        prefs.setOn(false);
        scheduler.cancel();
        notifier.cancelDaily();
    }

    /**
     * One daily check, for the phone's local date {@code today}. ReminderWorker calls it in the
     * background. It shows at most one daily reminder per local day, with up to two of the
     * items that cause it (see ReminderList).
     */
    @WorkerThread
    @NonNull
    public synchronized DailyResult runDailyCheck(@NonNull LocalDate today) {
        ReminderCheck.Decision decision;
        try {
            decision = ReminderCheck.check(prefs.isOn(), datePolicy, notifier.canPost(),
                    prefs.lastShownDay(), today, batches, thresholds.get());
        } catch (RuntimeException rulesError) {
            Log.e(TAG, "The date rules stopped the daily check", rulesError);
            return DailyResult.RULES_ERROR;
        }
        switch (decision.getOutcome()) {
            case SHOW:
                // The day is recorded on disk first. If the app stops right after this,
                // a restart finds the record, so the day can't get a second reminder.
                if (!prefs.recordShown(today)) {
                    return DailyResult.NOT_SHOWN;
                }
                return notifier.showDaily(decision.getList()) ? DailyResult.SHOWN
                        : DailyResult.NOT_SHOWN;
            case NOTHING_TO_REVIEW:
                notifier.cancelDaily();
                return DailyResult.NOTHING_TO_REVIEW;
            case ALREADY_SHOWN_TODAY:
                return DailyResult.ALREADY_SHOWN_TODAY;
            case NOTIFICATIONS_BLOCKED:
                return DailyResult.NOTIFICATIONS_BLOCKED;
            case RULES_NOT_READY:
                return DailyResult.RULES_NOT_READY;
            default:
                return DailyResult.REMINDERS_OFF;
        }
    }

    /**
     * Home calls this after it made its review list (with the same date rules, settings, and
     * date). When nothing in it causes a reminder any more, a daily reminder that is still shown
     * is removed at once. Otherwise a shown reminder is brought up to date soon, on this object's
     * own thread (since 2026-10-23; see {@link #refreshShownSoon}). It never shows one: the
     * household is looking at the list already.
     */
    public void onReviewListShown(@NonNull List<BatchWithStatus> reviewList) {
        if (!ReminderCheck.anyCausesReminder(reviewList)) {
            notifier.cancelDaily();
        } else {
            refreshShownSoon();
        }
    }

    /**
     * Brings a reminder that is still shown up to date soon, on this object's own thread: its
     * names and the number for the app icon, for today. Home calls it through
     * {@link #onReviewListShown}, and every saved change to the batches calls it. It never shows
     * a reminder that isn't shown.
     */
    public void refreshShownSoon() {
        background.execute(() -> {
            try {
                refreshShown(LocalDate.now());
            } catch (RuntimeException error) {
                // A refresh problem must never stop the app.
                Log.e(TAG, "The shown reminder could not be refreshed", error);
            }
        });
    }

    /**
     * Brings the reminder that is still shown up to date for {@code today} (since 2026-10-23): the
     * same list as the daily check, with the number for the app icon. It doesn't pop up or make a
     * sound again (see ReminderNotifier.updateIfShown). When nothing causes a reminder any more,
     * the reminder is removed. While no reminder is shown, nothing is read.
     */
    @VisibleForTesting
    @WorkerThread
    @NonNull
    synchronized RefreshResult refreshShown(@NonNull LocalDate today) {
        if (datePolicy == null) {
            return RefreshResult.RULES_NOT_READY;
        }
        if (!notifier.isShown()) {
            return RefreshResult.NOTHING_SHOWN;
        }
        ReminderList list;
        try {
            // The same list as the daily check (see ReminderCheck.check).
            list = ReminderList.of(RuleRunner.needsReview(batches.get(), datePolicy, today,
                    thresholds.get()));
        } catch (RuntimeException rulesError) {
            Log.e(TAG, "The date rules stopped the reminder's refresh", rulesError);
            return RefreshResult.RULES_ERROR;
        }
        if (list.isEmpty()) {
            notifier.cancelDaily();
            return RefreshResult.REMOVED;
        }
        switch (notifier.updateIfShown(list)) {
            case UPDATED:
                return RefreshResult.UPDATED;
            case UNCHANGED:
                return RefreshResult.UNCHANGED;
            case NOT_SHOWN:
                return RefreshResult.NOTHING_SHOWN;
            default:
                return RefreshResult.NOT_UPDATED;
        }
    }

    /**
     * Settings' "Show reminder now": {@link #showNow} for today, on this object's own thread.
     * Then it gives the result to {@code answer}, on that thread too.
     */
    public void showNowInBackground(@NonNull Consumer<NowResult> answer) {
        background.execute(() -> answer.accept(showNow(LocalDate.now())));
    }

    /**
     * Shows the reminder for {@code today} right away: the daily reminder's notification, with the
     * same items, words, and icon number, in its place (a new one replaces it). Since 2026-10-23 it
     * replaces the test reminder, the student's choice. It works while the daily reminder is off
     * too.
     *
     * <p>It is never recorded (also the student's choice): "Last reminder" keeps its day, and the
     * daily check still comes as usual. When nothing causes a reminder, it shows nothing and
     * removes a reminder that is still shown, like the daily check.
     */
    @WorkerThread
    @NonNull
    public synchronized NowResult showNow(@NonNull LocalDate today) {
        if (datePolicy == null) {
            return NowResult.RULES_NOT_READY;
        }
        ReminderList list;
        try {
            // The same list as the daily check (see ReminderCheck.check).
            list = ReminderList.of(RuleRunner.needsReview(batches.get(), datePolicy, today,
                    thresholds.get()));
        } catch (RuntimeException rulesError) {
            Log.e(TAG, "The date rules stopped \"Show reminder now\"", rulesError);
            return NowResult.RULES_ERROR;
        }
        if (list.isEmpty()) {
            notifier.cancelDaily();
            return NowResult.NOTHING_TO_REVIEW;
        }
        return notifier.showDaily(list) ? NowResult.SHOWN : NowResult.NOT_SHOWN;
    }
}

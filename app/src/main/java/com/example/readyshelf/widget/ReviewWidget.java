package com.example.readyshelf.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.ui.home.HomeActivity;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Keeps the home-screen widget ("Review list") up to date. AppContainer holds one object of this
 * class.
 *
 * <p>Like the daily reminder, the widget always shows the household's own records, never the
 * sample data. It never names items, because anyone who sees the phone's home screen can read it.
 * Since 2026-10-23 (the student's choice) it gives one number, Home's status tiles added up, the
 * same number as on the app icon; until then it gave no counts. All work happens on one background
 * thread, one refresh at a time, and each refresh reads the newest records. While no widget is
 * placed, nothing is read.
 */
public final class ReviewWidget {

    private static final String TAG = "ReviewWidget";
    // The reminder notifications use request code 0 for their tap.
    private static final int TAP_REQUEST_CODE = 1;

    private final Context context;
    private final Supplier<List<BatchInfo>> batches;
    private final Supplier<DateThresholds> thresholds;
    private final Consumer<Runnable> batchChanges;
    @Nullable private final DatePolicyContract datePolicy;
    // One background thread, so refreshes happen one at a time, in order.
    private final ExecutorService background = Executors.newSingleThreadExecutor();

    // What the placed widgets say now, or null when that isn't known. Guarded by "this".
    @Nullable private ReviewWidgetState shown;
    // True once this object hears about changes to the household's batches. Guarded by "this".
    private boolean listening;

    /**
     * @param batches      gives the household's batches that are not archived; called on the
     *                     widget's thread
     * @param thresholds   gives the household's planning settings
     * @param batchChanges starts calling the given Runnable after each saved change to the
     *                     household's batches; called once, on the widget's thread
     * @param datePolicy   the student's date rules, or null while they are not connected
     */
    public ReviewWidget(@NonNull Context context, @NonNull Supplier<List<BatchInfo>> batches,
                        @NonNull Supplier<DateThresholds> thresholds,
                        @NonNull Consumer<Runnable> batchChanges,
                        @Nullable DatePolicyContract datePolicy) {
        this.context = context.getApplicationContext();
        this.batches = Objects.requireNonNull(batches, "batches");
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds");
        this.batchChanges = Objects.requireNonNull(batchChanges, "batchChanges");
        this.datePolicy = datePolicy;
    }

    /** Refreshes the widget soon, on its own thread. Safe to call from any thread. */
    public void refreshSoon() {
        background.execute(() -> refreshNow(false));
    }

    /**
     * Sends the widget's words and tap again, even when the words didn't change, then calls
     * {@code done}. The widget provider uses it, so a new or restored widget never keeps the
     * layout's first words.
     */
    public void updateAll(@NonNull Runnable done) {
        background.execute(() -> {
            try {
                refreshNow(true);
            } finally {
                done.run();
            }
        });
    }

    /**
     * Gives the widget a new tap. Home calls it after a tap opened it: a second tap with the
     * same intent would only bring ReadyShelf to the front, on the screen that was open (see
     * HomeActivity.newReviewIntent).
     */
    public void renewTap() {
        background.execute(() -> refreshNow(true));
    }

    /**
     * Works out what the widget says, and sends it to every placed widget: always when
     * {@code always} is true, otherwise only when the words or the number changed. Background
     * thread only.
     */
    @WorkerThread
    public synchronized void refreshNow(boolean always) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            if (manager == null) {
                return; // this phone has no home-screen widgets
            }
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(context, ReviewWidgetProvider.class));
            if (ids == null || ids.length == 0) {
                shown = null; // no widget is placed, so nothing is read
                return;
            }
            ReviewWidgetState state = currentState();
            if (always || !state.equals(shown)) {
                manager.updateAppWidget(ids, views(state));
                shown = state;
            }
        } catch (RuntimeException error) {
            // A widget problem must never stop the app, the daily check, or the widget's update.
            Log.e(TAG, "The widget could not be refreshed", error);
        }
    }

    @WorkerThread
    @NonNull
    private ReviewWidgetState currentState() {
        try {
            if (!listening) {
                // Listen first, then read: a change saved in between causes one more refresh.
                batchChanges.accept(this::refreshSoon);
                listening = true;
            }
            return ReviewWidgetState.check(datePolicy, batches, LocalDate.now(), thresholds.get());
        } catch (RuntimeException error) {
            Log.e(TAG, "The widget could not work out the review list", error);
            return ReviewWidgetState.UNKNOWN;
        }
    }

    /** The widget's views for one state: the app name, one line, and the tap. */
    @VisibleForTesting
    @NonNull
    RemoteViews views(@NonNull ReviewWidgetState state) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_review);
        views.setTextViewText(R.id.widget_review_line, lineOf(context, state));
        // A new review intent each time (see renewTap). Tapping anywhere opens Home.
        PendingIntent tap = PendingIntent.getActivity(context, TAP_REQUEST_CODE,
                HomeActivity.newReviewIntent(context), PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(android.R.id.background, tap);
        return views;
    }

    /**
     * The words for each state, for example "5 household items need review". The only number is
     * Home's status tiles added up (since 2026-10-23). When every item only has a date to review,
     * that number is 0, so the line says "Household items need review" without one. The words
     * never name items or say that food is safe.
     */
    @NonNull
    static String lineOf(@NonNull Context context, @NonNull ReviewWidgetState state) {
        switch (state.getKind()) {
            case ITEMS_TO_REVIEW:
                if (state.getNumber() > 0) {
                    return context.getResources().getQuantityString(R.plurals.widget_review_count,
                            state.getNumber(), state.getNumber());
                }
                return context.getString(R.string.reminder_daily_title);
            case NOTHING_TO_REVIEW:
                return context.getString(R.string.home_needs_review_empty);
            default:
                return context.getString(R.string.reminder_daily_text);
        }
    }
}

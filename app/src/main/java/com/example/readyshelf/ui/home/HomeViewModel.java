package com.example.readyshelf.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.BatchWithStatus;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.ReviewCounts;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.domain.RuleRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Prepares what the Home screen shows. It survives screen rotation.
 * The rules run here (through RuleRunner), never in the Activity.
 *
 * <p>The status tiles at the top count every batch that needs review by status, medicine too.
 * Since 2026-10-22 (the student's choice) Home has no review lists: a tile opens Inventory with
 * its batches, so medicine with a passed date is under "Date passed", and so on. Medicine is
 * never swapped. The swaps are made again whenever the batches, the Settings numbers, or
 * the date change, so a confirmed swap or any stock change refreshes them.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";

    /** Everything the Home screen shows. A new State is made each time something changes. */
    public static final class State {
        public boolean hasBatches;
        public boolean hasSampleData;
        /** True while the separate sample data is open; every screen then shows its label. */
        public boolean sampleDataOpen;
        /** The status tiles' counts. An empty list shows no status tiles. */
        @NonNull public List<ReviewCounts.StatusCount> reviewCounts = new ArrayList<>();
        // The note under the tiles: "Nothing needs review right now." or why the date rules
        // gave no counts.
        @StringRes public int reviewMessage;     // 0 means "no message"
        @Nullable public String reviewError;     // set when the date rules threw an error
        @NonNull public List<RuleRunner.ShownSwap> swaps = new ArrayList<>();
        @StringRes public int swapsMessage;      // 0 means "no message"
        @Nullable public String swapsError;      // set when the rules threw an error
    }

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final LiveData<List<BatchInfo>> batches;
    private final LiveData<DateThresholds> thresholds;
    private final MutableLiveData<LocalDate> today = new MutableLiveData<>(LocalDate.now());
    private final MediatorLiveData<State> state = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        batches = data.repository.observeBatches();
        thresholds = data.settings.observeThresholds();
        // Make a new State when the batches, the Settings numbers, or the date change.
        state.addSource(batches, value -> update());
        state.addSource(thresholds, value -> update());
        state.addSource(today, value -> update());
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    /** The Activity calls this when it comes back to the front, in case the date changed. */
    public void refreshToday() {
        LocalDate now = LocalDate.now();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
    }

    /**
     * The Activity calls this after a reminder or widget tap opened Home. The widget then gets a
     * new tap (see ReviewWidget.renewTap).
     */
    public void onReviewIntent() {
        app.reviewWidget.renewTap();
    }

    private void update() {
        List<BatchInfo> allBatches = batches.getValue();
        DateThresholds currentThresholds = thresholds.getValue();
        LocalDate currentDay = today.getValue();
        if (allBatches == null || currentThresholds == null || currentDay == null) {
            return; // still loading
        }
        // The home-screen widget shows the household's records. It reads them itself, on its own
        // thread; this covers a new day and changed Settings numbers.
        if (!data.sample) {
            app.reviewWidget.refreshSoon();
        }

        State next = new State();
        next.hasBatches = !allBatches.isEmpty();
        next.sampleDataOpen = data.sample;
        for (BatchInfo batch : allBatches) {
            next.hasSampleData |= batch.isSample();
        }

        DatePolicyContract policy = app.datePolicy;
        RotationPlannerContract planner = app.rotationPlanner;

        if (policy == null) {
            next.reviewMessage = R.string.rules_not_ready_date;
        } else {
            try {
                List<BatchWithStatus> reviewList =
                        RuleRunner.needsReview(allBatches, policy, currentDay, currentThresholds);
                // The daily reminder uses the same statuses. When nothing causes a reminder
                // any more (for example after a stock change), a shown reminder is removed;
                // otherwise its names and its number on the app icon are brought up to date.
                // The reminder is about the household's own records, so a list made from the
                // sample data never changes it.
                if (!data.sample) {
                    app.reminders.onReviewListShown(reviewList);
                }
                // The status tiles count every batch on this list by status, medicine too.
                next.reviewCounts = ReviewCounts.byStatus(reviewList);
                // With no batches at all, Home's getting-started message says enough.
                if (reviewList.isEmpty() && next.hasBatches) {
                    next.reviewMessage = R.string.home_needs_review_empty;
                }
            } catch (RuntimeException error) {
                // A mistake in the rules shows as text instead of closing the app.
                Log.e(TAG, "The date rules threw an error", error);
                next.reviewError = error.toString();
            }
        }

        if (planner == null) {
            next.swapsMessage = R.string.rules_not_ready_swaps;
        } else {
            try {
                next.swaps = RuleRunner.plan(allBatches, planner, currentDay, currentThresholds).getSwaps();
                // No note above the swap cards (the student's choice on 2026-10-21). Review swap
                // still says that the replacement days are a planning choice before any confirm.
                next.swapsMessage = next.swaps.isEmpty() ? R.string.home_swaps_empty : 0;
            } catch (RuntimeException error) {
                Log.e(TAG, "The swap rules threw an error", error);
                next.swapsError = error.toString();
            }
        }

        state.setValue(next);
    }
}

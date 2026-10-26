package com.example.readyshelf.ui.batch;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.BatchEvent;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.RuleRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Prepares one batch, its status, and its history for the Batch details screen. */
public class BatchDetailsViewModel extends AndroidViewModel {

    private static final String TAG = "BatchDetailsViewModel";

    /** Everything the details part of the screen shows. */
    public static final class State {
        @Nullable public BatchInfo batch;      // null when the batch no longer exists
        @Nullable public DateStatus status;    // null when there is no status to show
        public boolean dateRulesMissing;
        @Nullable public String rulesError;
    }

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final LiveData<DateThresholds> thresholds;
    private final MutableLiveData<LocalDate> today = new MutableLiveData<>(LocalDate.now());
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final MediatorLiveData<List<BatchEvent>> history = new MediatorLiveData<>();
    @Nullable private LiveData<BatchInfo> batch;
    private boolean batchLoaded;
    private boolean started;
    private long batchId;

    public BatchDetailsViewModel(@NonNull Application application) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        thresholds = data.settings.observeThresholds();
    }

    /** The Activity calls this in onCreate. Later calls do nothing. */
    public void start(long batchIdToShow) {
        if (started) {
            return;
        }
        started = true;
        batchId = batchIdToShow;
        batch = data.repository.observeBatchInfo(batchId);
        state.addSource(batch, value -> {
            batchLoaded = true;
            update();
        });
        state.addSource(thresholds, value -> update());
        state.addSource(today, value -> update());
        history.addSource(data.repository.observeHistory(batchId), events -> history.setValue(events));
    }

    public long getBatchId() {
        return batchId;
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    /** The batch's history, newest first. */
    @NonNull
    public LiveData<List<BatchEvent>> getHistory() {
        return history;
    }

    /** The Activity calls this when it comes back to the front, in case the date changed. */
    public void refreshToday() {
        LocalDate now = LocalDate.now();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
    }

    private void update() {
        DateThresholds currentThresholds = thresholds.getValue();
        LocalDate currentDay = today.getValue();
        if (!batchLoaded || batch == null || currentThresholds == null || currentDay == null) {
            return; // still loading
        }

        State next = new State();
        next.batch = batch.getValue();
        if (next.batch != null) {
            DatePolicyContract policy = app.datePolicy;
            if (policy == null) {
                next.dateRulesMissing = true;
            } else {
                try {
                    // No status for an archived batch: it is never given to the date rules.
                    next.status = RuleRunner.statusesOfActive(Collections.singletonList(next.batch),
                            policy, currentDay, currentThresholds).get(next.batch.getId());
                } catch (RuntimeException error) {
                    // A mistake in the rules shows as text instead of closing the app.
                    Log.e(TAG, "The date rules threw an error", error);
                    next.rulesError = error.toString();
                }
            }
        }
        state.setValue(next);
    }
}

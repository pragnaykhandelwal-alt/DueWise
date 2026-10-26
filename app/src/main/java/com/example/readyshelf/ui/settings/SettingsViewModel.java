package com.example.readyshelf.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.reminders.Reminders.NowResult;

import java.time.LocalDate;

/**
 * Reads and saves the planning settings, turns the daily reminder on or off, and shows the
 * reminder now (until 2026-10-23, a test reminder). Until 2026-10-22 it also opened and kept the
 * separate sample data; Settings has no Sample data card since then. Closing sample data is
 * SampleDataClose's job, because every screen can close it.
 */
public class SettingsViewModel extends AndroidViewModel {

    /** What to do after Android answers the notification permission question. */
    public enum PendingAction {
        NONE,
        /** Turn the daily reminder on. */
        ENABLE,
        /** Show the reminder now. */
        SHOW_NOW
    }

    // Kept in the SavedStateHandle, so the answer still arrives after Android closed and
    // reopened the app while the permission question was showing.
    private static final String KEY_PENDING_ACTION = "pendingAction";

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final SavedStateHandle saved;
    private final MutableLiveData<Integer> message = new MutableLiveData<>();
    private boolean thresholdFieldsFilled;

    public SettingsViewModel(@NonNull Application application, @NonNull SavedStateHandle savedState) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        saved = savedState;
    }

    @NonNull
    public LiveData<DateThresholds> getThresholds() {
        return data.settings.observeThresholds();
    }

    /** True when both of the student's rules are connected (see AppContainer). */
    public boolean hasAllRules() {
        return app.datePolicy != null && app.rotationPlanner != null;
    }

    /**
     * True only the first time it is called. The Activity fills the number fields once;
     * after the screen rotates, the fields keep the household's typing by themselves.
     */
    public boolean shouldFillThresholdFields() {
        if (thresholdFieldsFilled) {
            return false;
        }
        thresholdFieldsFilled = true;
        return true;
    }

    public void saveThresholds(@NonNull DateThresholds thresholds) {
        data.settings.saveThresholds(thresholds);
    }

    /**
     * True when this screen shows the separate sample data (see AppContainer). Then the planning
     * numbers are the sample data's own, and the Planning card says so.
     */
    public boolean isSampleDataOpen() {
        return data.sample;
    }

    // ----- Reminders -----

    /** True when the date rules are connected, so the daily reminder can be turned on. */
    public boolean areRemindersAvailable() {
        return app.reminders.isAvailable();
    }

    public boolean areRemindersOn() {
        return app.reminders.isOn();
    }

    /** True when Android lets ReadyShelf show notifications right now. */
    public boolean canShowNotifications() {
        return app.reminders.canPost();
    }

    /** The local day of the last daily reminder, or null for never. */
    @Nullable
    public LocalDate lastReminderDay() {
        return app.reminders.lastShownDay();
    }

    /** Returns false while the date rules are not connected. */
    public boolean turnRemindersOn() {
        return app.reminders.turnOn();
    }

    public void turnRemindersOff() {
        app.reminders.turnOff();
    }

    /**
     * Shows the reminder with the household's items now, in the background. Then
     * {@link #getMessage()} says what happened.
     */
    public void showReminderNow() {
        // This answer comes on the reminders' background thread.
        app.reminders.showNowInBackground(result -> message.postValue(messageFor(result)));
    }

    @StringRes
    private static int messageFor(@NonNull NowResult result) {
        switch (result) {
            case SHOWN:
                return R.string.settings_show_reminder_sent;
            case NOTHING_TO_REVIEW:
                return R.string.settings_show_reminder_nothing;
            case RULES_NOT_READY:
                return R.string.settings_reminders_off;
            case RULES_ERROR:
                return R.string.settings_show_reminder_failed;
            default:
                return R.string.settings_notifications_refused;
        }
    }

    /** A message for the screen to show once, or null. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    /** The screen showed the message, so it isn't shown again after the screen rotates. */
    public void messageShown() {
        message.setValue(null);
    }

    public void setPendingAction(@NonNull PendingAction action) {
        saved.set(KEY_PENDING_ACTION, action.name());
    }

    /** The action waiting for the permission answer, and forgets it. NONE when there is none. */
    @NonNull
    public PendingAction takePendingAction() {
        String name = saved.get(KEY_PENDING_ACTION);
        saved.remove(KEY_PENDING_ACTION);
        if (name == null) {
            return PendingAction.NONE;
        }
        try {
            return PendingAction.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return PendingAction.NONE;
        }
    }
}

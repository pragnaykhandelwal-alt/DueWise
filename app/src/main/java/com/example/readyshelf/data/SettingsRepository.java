package com.example.readyshelf.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.readyshelf.domain.DateThresholds;

import java.util.Map;

/**
 * Keeps the Settings numbers on the phone (in SharedPreferences, a small private file)
 * and shares them with the screens as LiveData.
 */
public final class SettingsRepository {

    /** The household's planning settings. The sample data has a file of its own (see DataSets). */
    public static final String FILE_NAME = "settings";

    private static final String NEAR_DATE_DAYS_KEY = "near_date_days";
    private static final String REPLACEMENT_DAYS_KEY = "replacement_days";
    // Before 2026-10-11 the file held one "review_days_<LOCATION TYPE>" number for each location
    // type. Those keys are no longer read, and their values are left in the file.

    private final SharedPreferences preferences;
    private final MutableLiveData<DateThresholds> thresholds;

    public SettingsRepository(@NonNull Context context, @NonNull String fileName) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(fileName, Context.MODE_PRIVATE);
        thresholds = new MutableLiveData<>(readThresholds());
    }

    @NonNull
    public LiveData<DateThresholds> observeThresholds() {
        return thresholds;
    }

    /** Saves new numbers and tells every screen. Call it on the main thread. */
    public void saveThresholds(@NonNull DateThresholds newThresholds) {
        preferences.edit()
                .putInt(NEAR_DATE_DAYS_KEY, newThresholds.getNearDateDays())
                .putInt(REPLACEMENT_DAYS_KEY, newThresholds.getReplacementDays())
                .apply();
        thresholds.setValue(newThresholds);
    }

    /**
     * The numbers saved right now. Unlike {@link #observeThresholds()}, this can be called on any
     * thread, so the swap save can read the current settings inside its database transaction.
     */
    @NonNull
    public DateThresholds current() {
        return readThresholds();
    }

    /**
     * The saved numbers. A missing or out-of-range number gets its starting value, and a pair
     * whose replacement days are not more than its near-date days gets both starting values
     * (see DateThresholds.fromSaved).
     */
    private DateThresholds readThresholds() {
        // One copy of everything saved, so both numbers come from the same save.
        Map<String, ?> saved = preferences.getAll();
        int nearDateDays = savedNumber(saved, NEAR_DATE_DAYS_KEY, DateThresholds.DEFAULT_NEAR_DATE_DAYS);
        int replacementDays = savedNumber(saved, REPLACEMENT_DAYS_KEY,
                DateThresholds.DEFAULT_REPLACEMENT_DAYS);
        return DateThresholds.fromSaved(nearDateDays, replacementDays);
    }

    private static int savedNumber(@NonNull Map<String, ?> saved, @NonNull String key,
                                   int startingValue) {
        Object value = saved.get(key);
        return value instanceof Integer ? (Integer) value : startingValue;
    }
}

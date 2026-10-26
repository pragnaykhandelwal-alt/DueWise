package com.example.readyshelf.reminders;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Keeps the daily reminder's switch and the record of the last daily reminder on the phone,
 * in a small private file (SharedPreferences). App backup is off, and the backup rules exclude
 * this file too.
 *
 * <p>The record is the local day of the last daily reminder, for example "2026-10-11". It is
 * written to disk before the reminder is shown, so a restart can never show a second daily
 * reminder on the same day.
 */
public final class ReminderPrefs {

    /** The file the app uses. Tests use a file of their own. */
    public static final String FILE_NAME = "reminders";

    private static final String KEY_ON = "daily_reminder_on";
    private static final String KEY_LAST_SHOWN_DAY = "last_daily_reminder_day";

    private final SharedPreferences preferences;

    public ReminderPrefs(@NonNull Context context, @NonNull String fileName) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    /** True when the household turned the daily reminder on. It starts off. */
    public boolean isOn() {
        return preferences.getBoolean(KEY_ON, false);
    }

    public void setOn(boolean on) {
        preferences.edit().putBoolean(KEY_ON, on).apply();
    }

    /** The local day of the last daily reminder, or null when none was shown yet. */
    @Nullable
    public LocalDate lastShownDay() {
        String text = preferences.getString(KEY_LAST_SHOWN_DAY, null);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException unreadable) {
            return null;
        }
    }

    /**
     * Records that the daily reminder of {@code day} is shown. It waits until the file is
     * written (commit), and returns false when that failed.
     */
    public boolean recordShown(@NonNull LocalDate day) {
        return preferences.edit().putString(KEY_LAST_SHOWN_DAY, day.toString()).commit();
    }
}

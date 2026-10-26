package com.example.readyshelf.reminders;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.ReadyShelfApp;

import java.time.LocalDate;

/**
 * The daily reminder check that WorkManager runs in the background (see ReminderScheduler).
 * WorkManager makes this object itself, so the class and its constructor must stay public and
 * the class must keep its name.
 */
public class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppContainer app = ReadyShelfApp.containerOf((Application) getApplicationContext());
        // The phone's local calendar date, read now, for this check only.
        Reminders.DailyResult result = app.reminders.runDailyCheck(LocalDate.now());
        Log.i(TAG, "Daily check: " + result);
        // The home-screen widget, if one is placed, agrees with today's check.
        app.reviewWidget.refreshNow(false);
        // Always success: a retry would only repeat the same answer. The next check comes as usual.
        return Result.success();
    }
}

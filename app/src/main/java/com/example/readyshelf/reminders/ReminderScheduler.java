package com.example.readyshelf.reminders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.readyshelf.domain.ReminderTiming;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Schedules the one daily reminder check with WorkManager, or cancels it.
 *
 * <p>The check has a fixed unique name, and scheduling keeps a check that already exists (KEEP),
 * so calling this again, for example at every app start, never adds a second worker.
 * Android decides when each check really runs; there is no exact time and no exact alarm.
 *
 * <p>Notes for later changes:
 * <ul>
 *   <li>Never use setExpedited or setForeground: the manifest removes FOREGROUND_SERVICE.</li>
 *   <li>KEEP keeps an old schedule as it is. To change the interval or the worker later, schedule
 *       once with ExistingPeriodicWorkPolicy.UPDATE.</li>
 *   <li>Never rename ReminderWorker: WorkManager stores the class name with the schedule.</li>
 * </ul>
 */
public final class ReminderScheduler {

    /** The unique name of the app's daily check. */
    public static final String UNIQUE_WORK_NAME = "readyshelf-daily-review-check";

    private final Context context;
    private final String uniqueName;
    private final Supplier<ZonedDateTime> clock;

    public ReminderScheduler(@NonNull Context context) {
        this(context, UNIQUE_WORK_NAME, ZonedDateTime::now);
    }

    /** For tests: another unique name, and a fixed clock for the first wait. */
    ReminderScheduler(@NonNull Context context, @NonNull String uniqueName,
                      @NonNull Supplier<ZonedDateTime> clock) {
        this.context = context.getApplicationContext();
        this.uniqueName = uniqueName;
        this.clock = clock;
    }

    /**
     * Makes sure the daily check is scheduled, once. The first check waits until about 9:00 in
     * the morning (see ReminderTiming); each later check comes about a day after the one before.
     * The returned Operation finishes when WorkManager has saved it (tests wait for that).
     */
    @NonNull
    public Operation ensureScheduled() {
        long firstWaitMillis = ReminderTiming.firstRunDelay(clock.get()).toMillis();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReminderWorker.class,
                1, TimeUnit.DAYS)
                .setInitialDelay(firstWaitMillis, TimeUnit.MILLISECONDS)
                .build();
        return WorkManager.getInstance(context).enqueueUniquePeriodicWork(uniqueName,
                ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** Cancels the daily check. Nothing more is scheduled until reminders are turned on again. */
    @NonNull
    public Operation cancel() {
        return WorkManager.getInstance(context).cancelUniqueWork(uniqueName);
    }
}

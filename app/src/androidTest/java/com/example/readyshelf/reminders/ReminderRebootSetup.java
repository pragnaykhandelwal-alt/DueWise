package com.example.readyshelf.reminders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Not a normal test: it prepares and cleans up the reboot check of TOOLCHAIN.md, on the
 * emulator only. It schedules a test check that waits two days (so it never runs), which must
 * still be scheduled after the emulator restarts:
 * <pre>
 * ... am instrument -w -e rebootCheck schedule -e class com.example.readyshelf.reminders.ReminderRebootSetup ...
 * ... am instrument -w -e rebootCheck cancel -e class com.example.readyshelf.reminders.ReminderRebootSetup ...
 * </pre>
 * In normal test runs it is skipped.
 */
@RunWith(AndroidJUnit4.class)
public class ReminderRebootSetup {

    static final String NAME = "readyshelf-reboot-check";

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void scheduleOrCancelTheRebootCheck() throws Exception {
        String mode = InstrumentationRegistry.getArguments().getString("rebootCheck");
        assumeTrue("Runs only with -e rebootCheck schedule|cancel (see the class comment)",
                "schedule".equals(mode) || "cancel".equals(mode));
        WorkManager workManager = WorkManager.getInstance(context);
        if ("schedule".equals(mode)) {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReminderWorker.class,
                    1, TimeUnit.DAYS)
                    .setInitialDelay(2, TimeUnit.DAYS)
                    .build();
            workManager.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
                    .getResult().get();
            List<WorkInfo> work = workManager.getWorkInfosForUniqueWork(NAME).get();
            assertEquals(1, work.size());
            assertEquals(WorkInfo.State.ENQUEUED, work.get(0).getState());
        } else {
            workManager.cancelUniqueWork(NAME).getResult().get();
        }
    }
}

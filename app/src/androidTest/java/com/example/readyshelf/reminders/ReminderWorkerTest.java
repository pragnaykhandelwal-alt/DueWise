package com.example.readyshelf.reminders;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * WorkManager can make and run ReminderWorker in the app, with the app's real AppContainer,
 * and without the ACCESS_NETWORK_STATE and FOREGROUND_SERVICE permissions (the manifest removes
 * them). Reminders are off in a fresh install, so the check ends without showing anything.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReminderWorkerTest {

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void oneCheck_runsToSuccess() throws Exception {
        WorkManager workManager = WorkManager.getInstance(context);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class).build();

        workManager.enqueue(request).getResult().get();

        WorkInfo.State state = null;
        long end = SystemClock.uptimeMillis() + 60_000;
        while (SystemClock.uptimeMillis() < end) {
            state = workManager.getWorkInfoById(request.getId()).get().getState();
            if (state.isFinished()) {
                break;
            }
            SystemClock.sleep(200);
        }
        assertEquals(WorkInfo.State.SUCCEEDED, state);
    }
}

package com.example.readyshelf.reminders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The daily check's schedule with the real WorkManager on the emulator: scheduling twice (or
 * again after an app restart) keeps one worker, and cancelling leaves none. The test uses its
 * own unique name, and a first wait of almost a day, so the worker never runs during the test.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReminderSchedulerTest {

    private static final String NAME = "readyshelf-scheduler-test";

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    @After
    public void cancelTestSchedule() throws Exception {
        WorkManager.getInstance(context).cancelUniqueWork(NAME).getResult().get();
    }

    @Test
    public void schedulingTwice_keepsOneWorker() throws Exception {
        ReminderScheduler scheduler = scheduler();

        scheduler.ensureScheduled().getResult().get();
        UUID first = onlyUnfinished().getId();
        scheduler.ensureScheduled().getResult().get();

        WorkInfo info = onlyUnfinished();
        assertEquals(first, info.getId());
        assertEquals(WorkInfo.State.ENQUEUED, info.getState());
    }

    @Test
    public void afterAnAppRestart_theSameWorkerIsKept() throws Exception {
        scheduler().ensureScheduled().getResult().get();
        UUID first = onlyUnfinished().getId();

        // A new scheduler object, as after the app starts again.
        scheduler().ensureScheduled().getResult().get();

        assertEquals(first, onlyUnfinished().getId());
    }

    @Test
    public void cancelling_leavesNoWorker_andTurningOnAgainMakesExactlyOne() throws Exception {
        ReminderScheduler scheduler = scheduler();
        scheduler.ensureScheduled().getResult().get();
        UUID first = onlyUnfinished().getId();

        scheduler.cancel().getResult().get();

        List<WorkInfo> afterCancel = all();
        assertFalse(afterCancel.isEmpty());
        for (WorkInfo info : afterCancel) {
            assertEquals(WorkInfo.State.CANCELLED, info.getState());
        }

        scheduler.ensureScheduled().getResult().get();
        assertNotEquals(first, onlyUnfinished().getId());
    }

    // ----- Helpers -----

    @NonNull
    private ReminderScheduler scheduler() {
        // 10:00 in the morning: the first check would wait until 9:00 the next day.
        return new ReminderScheduler(context, NAME,
                () -> ZonedDateTime.of(2026, 10, 11, 10, 0, 0, 0, ZoneId.systemDefault()));
    }

    @NonNull
    private List<WorkInfo> all() throws Exception {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWork(NAME).get();
    }

    /** The one unfinished worker with the test name. Fails when there are none or several. */
    @NonNull
    private WorkInfo onlyUnfinished() throws Exception {
        List<WorkInfo> unfinished = new ArrayList<>();
        for (WorkInfo info : all()) {
            if (!info.getState().isFinished()) {
                unfinished.add(info);
            }
        }
        assertEquals("unfinished workers named " + NAME, 1, unfinished.size());
        return unfinished.get(0);
    }
}

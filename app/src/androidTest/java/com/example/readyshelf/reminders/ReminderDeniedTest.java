package com.example.readyshelf.reminders;

import static com.example.readyshelf.reminders.NotificationChecks.assertStaysAbsent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;

/**
 * Without the notification permission nothing is shown and nothing is recorded.
 *
 * <p>Taking the permission away stops the app's process, so this test can't do it itself. It
 * runs only on purpose, on the emulator, after the permission was taken away:
 * <pre>
 * adb -s emulator-5554 shell pm revoke com.example.readyshelf android.permission.POST_NOTIFICATIONS
 * adb -s emulator-5554 shell am instrument -w -e notifications denied \
 *     -e class com.example.readyshelf.reminders.ReminderDeniedTest \
 *     com.example.readyshelf.test/androidx.test.runner.AndroidJUnitRunner
 * </pre>
 * In normal test runs it is skipped.
 */
@RunWith(AndroidJUnit4.class)
public class ReminderDeniedTest {

    private static final String PREFS_FILE = "reminders-denied-test";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @After
    public void deleteTestFile() {
        context.deleteSharedPreferences(PREFS_FILE);
    }

    @Test
    public void withoutPermission_nothingIsShownOrRecorded() {
        assumeTrue("Runs only with -e notifications denied (see the class comment)",
                "denied".equals(InstrumentationRegistry.getArguments().getString("notifications")));
        ReminderNotifier notifier = new ReminderNotifier(context);
        notifier.createChannel();
        BatchInfo medicine = new BatchInfo(1, "Pain reliever tablets", ItemCategory.MEDICINE, 10,
                "tablets", DAY.plusDays(3), DateLabel.EXPIRES, 3, "First aid box",
                LocationType.FIRST_AID, false);
        ReminderPrefs prefs = new ReminderPrefs(context, PREFS_FILE);
        prefs.setOn(true);
        Reminders reminders = new Reminders(prefs, notifier,
                new ReminderScheduler(context, "readyshelf-denied-test",
                        () -> ZonedDateTime.of(2026, 10, 11, 10, 0, 0, 0, ZoneId.systemDefault())),
                () -> Collections.singletonList(medicine), () -> DateThresholds.of(30, 180),
                listener -> { }, new FixedStatusPolicy().status(medicine, DateStatus.NEAR_DATE));

        assertFalse(notifier.canPost());
        assertEquals(Reminders.NowResult.NOT_SHOWN, reminders.showNow(DAY));
        assertEquals(Reminders.DailyResult.NOTIFICATIONS_BLOCKED, reminders.runDailyCheck(DAY));

        assertNull(prefs.lastShownDay());
        assertStaysAbsent(context, ReminderNotifier.DAILY_ID);
    }
}

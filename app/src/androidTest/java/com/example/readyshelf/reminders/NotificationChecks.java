package com.example.readyshelf.reminders;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * Helpers for the reminder tests on the emulator. Android shows notifications a moment after
 * they are posted, so the checks wait (up to 5 seconds) instead of looking only once.
 */
final class NotificationChecks {

    private static final long WAIT_MILLIS = 5000;
    private static final long STEP_MILLIS = 100;

    private NotificationChecks() {
        // Only static methods.
    }

    /**
     * Allows ReadyShelf's notifications on Android 13 and newer, like tapping "Allow".
     * (Taking the permission away would stop the test process, so tests never do that.)
     */
    static void allowNotifications(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(context.getPackageName(),
                            Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** ReadyShelf's shown notification with this ID, or null. */
    @Nullable
    static StatusBarNotification shown(@NonNull Context context, int id) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            if (notification.getId() == id) {
                return notification;
            }
        }
        return null;
    }

    /** How many of ReadyShelf's shown notifications have this ID. */
    static int countShown(@NonNull Context context, int id) {
        int count = 0;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            if (notification.getId() == id) {
                count++;
            }
        }
        return count;
    }

    /** Waits until the notification is shown, and returns it. */
    @NonNull
    static StatusBarNotification waitUntilShown(@NonNull Context context, int id) {
        long end = SystemClock.uptimeMillis() + WAIT_MILLIS;
        while (SystemClock.uptimeMillis() < end) {
            StatusBarNotification notification = shown(context, id);
            if (notification != null) {
                return notification;
            }
            SystemClock.sleep(STEP_MILLIS);
        }
        fail("Notification " + id + " was not shown within " + WAIT_MILLIS + " ms");
        throw new AssertionError(); // not reached
    }

    /**
     * Waits until the notification is shown with {@code number} (the number for the app icon),
     * and returns it. Used for the quiet updates, which come a moment after a change.
     */
    @NonNull
    static StatusBarNotification waitUntilNumber(@NonNull Context context, int id, int number) {
        long end = SystemClock.uptimeMillis() + WAIT_MILLIS;
        while (SystemClock.uptimeMillis() < end) {
            StatusBarNotification notification = shown(context, id);
            if (notification != null && notification.getNotification().number == number) {
                return notification;
            }
            SystemClock.sleep(STEP_MILLIS);
        }
        StatusBarNotification last = shown(context, id);
        fail("Notification " + id + " did not get the number " + number + " within "
                + WAIT_MILLIS + " ms; it has "
                + (last == null ? "gone" : String.valueOf(last.getNotification().number)));
        throw new AssertionError(); // not reached
    }

    /** Waits until the notification is gone. */
    static void waitUntilGone(@NonNull Context context, int id) {
        long end = SystemClock.uptimeMillis() + WAIT_MILLIS;
        while (SystemClock.uptimeMillis() < end) {
            if (shown(context, id) == null) {
                return;
            }
            SystemClock.sleep(STEP_MILLIS);
        }
        fail("Notification " + id + " was still shown after " + WAIT_MILLIS + " ms");
    }

    /** Checks that the notification stays absent for a second (it could appear late). */
    static void assertStaysAbsent(@NonNull Context context, int id) {
        SystemClock.sleep(1000);
        assertNull("Notification " + id + " should not be shown", shown(context, id));
    }

    /**
     * Removes the reminder notification and waits until it is gone. A post that Android is
     * still finishing can come in after a cancel, so it cancels again when needed (up to 3 times).
     * (Until 2026-10-23 there was a test reminder too.)
     */
    static void removeReminders(@NonNull Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (int attempt = 0; attempt < 3; attempt++) {
            manager.cancel(ReminderNotifier.DAILY_ID);
            long end = SystemClock.uptimeMillis() + 2000;
            while (SystemClock.uptimeMillis() < end) {
                if (shown(context, ReminderNotifier.DAILY_ID) == null) {
                    return;
                }
                SystemClock.sleep(STEP_MILLIS);
            }
        }
        fail("The reminder notification was still shown after 3 cancels");
    }
}

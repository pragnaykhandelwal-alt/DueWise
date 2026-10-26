package com.example.readyshelf.reminders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.R;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The reminder channel, on the emulator: it pops up with a sound (high importance), it never
 * bypasses Do Not Disturb, and it replaced the old silent channel. No permission is needed.
 *
 * <p>Most tests use their own channel IDs, so a test never turns off the real channel. (Android
 * never makes a channel louder again, so the other reminder tests would fail until ReadyShelf
 * was installed again.) Each test ID always gets the same importance, because Android brings back
 * a deleted channel with its last settings.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReminderChannelTest {

    private static final String OLD_SILENT = "test_old_silent";
    private static final String NEW_FROM_SILENT = "test_new_from_silent";
    private static final String OLD_TURNED_OFF = "test_old_turned_off";
    private static final String NEW_FROM_TURNED_OFF = "test_new_from_turned_off";

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final NotificationManagerCompat manager = NotificationManagerCompat.from(context);

    @After
    public void tearDown() {
        for (String id : new String[] {OLD_SILENT, NEW_FROM_SILENT, OLD_TURNED_OFF,
                NEW_FROM_TURNED_OFF}) {
            manager.deleteNotificationChannel(id);
        }
    }

    @Test
    public void theRemindersChannel_popsUpWithSound_andNeverBypassesDoNotDisturb() {
        new ReminderNotifier(context).createChannel();

        NotificationChannelCompat channel =
                manager.getNotificationChannelCompat(ReminderNotifier.CHANNEL_ID);
        assertNotNull(channel);
        assertEquals("If \"Review reminders\" was changed by hand on the emulator, install "
                        + "ReadyShelf on the emulator again (never on the phone)",
                NotificationManagerCompat.IMPORTANCE_HIGH, channel.getImportance());
        assertEquals(context.getString(R.string.reminder_channel_name),
                String.valueOf(channel.getName()));
        assertEquals(context.getString(R.string.reminder_channel_description),
                channel.getDescription());
        assertNotNull("The channel plays the phone's notification sound", channel.getSound());
        assertFalse(channel.canBypassDnd());
        assertNull(manager.getNotificationChannelCompat(ReminderNotifier.OLD_SILENT_CHANNEL_ID));
    }

    @Test
    public void replacingASilentChannel_deletesIt_andTheNewOneHasHighImportance() {
        create(OLD_SILENT, NotificationManagerCompat.IMPORTANCE_LOW);

        ReminderNotifier.replaceChannel(context, NEW_FROM_SILENT, OLD_SILENT);

        assertImportance(NotificationManagerCompat.IMPORTANCE_HIGH, NEW_FROM_SILENT);
        assertNull(manager.getNotificationChannelCompat(OLD_SILENT));
    }

    @Test
    public void aChannelTheHouseholdTurnedOff_staysOffInTheNewChannel() {
        create(OLD_TURNED_OFF, NotificationManagerCompat.IMPORTANCE_NONE);

        ReminderNotifier.replaceChannel(context, NEW_FROM_TURNED_OFF, OLD_TURNED_OFF);

        assertImportance(NotificationManagerCompat.IMPORTANCE_NONE, NEW_FROM_TURNED_OFF);
        assertNull(manager.getNotificationChannelCompat(OLD_TURNED_OFF));
    }

    @Test
    public void replacingAgain_changesNothing() {
        create(OLD_SILENT, NotificationManagerCompat.IMPORTANCE_LOW);
        ReminderNotifier.replaceChannel(context, NEW_FROM_SILENT, OLD_SILENT);

        ReminderNotifier.replaceChannel(context, NEW_FROM_SILENT, OLD_SILENT);

        assertImportance(NotificationManagerCompat.IMPORTANCE_HIGH, NEW_FROM_SILENT);
        assertNull(manager.getNotificationChannelCompat(OLD_SILENT));
    }

    // ----- Helpers -----

    private void create(@NonNull String id, int importance) {
        manager.createNotificationChannel(new NotificationChannelCompat.Builder(id, importance)
                .setName("Test channel " + id)
                .build());
    }

    private void assertImportance(int expected, @NonNull String id) {
        NotificationChannelCompat channel = manager.getNotificationChannelCompat(id);
        assertNotNull(id, channel);
        assertEquals(id, expected, channel.getImportance());
    }
}

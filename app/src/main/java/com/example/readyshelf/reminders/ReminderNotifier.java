package com.example.readyshelf.reminders;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.ReminderList;
import com.example.readyshelf.ui.home.HomeActivity;

import java.util.List;

/**
 * Shows ReadyShelf's one notification: the reminder, from the daily check or from Settings'
 * "Show reminder now". Until 2026-10-23 Settings sent a separate test reminder instead.
 *
 * <p>The reminder lists up to two items that need review and how many others there are, for
 * example "Canned beans, Granola bars, and 3 others" (since 2026-10-19, the student's choice;
 * "3 more" until 2026-10-23; see ReminderList). It never names medicine. Its lock-screen (public)
 * version has only the generic words. On a locked phone that hides sensitive notification
 * content, Android shows only that version; when the phone shows all content there, the names
 * appear on the lock screen too. So the household's lock-screen setting decides.
 *
 * <p>Since 2026-10-23 (the student's choice) the reminder also carries the number for the app
 * icon: Home's status tiles added up (Notification.number). Android has no other way for an app to
 * put a number on its icon: the home screen shows it only while the reminder is shown, and the home
 * screen decides how. Some show the number on the icon; others (like the Moto and Pixel home
 * screens) show a dot, and the number when the icon is long-pressed. While the reminder is shown,
 * {@link #updateIfShown} keeps its words and number up to date, quietly.
 *
 * <p>The notification has a fixed ID, so showing it again replaces it instead of adding a
 * second one. The channel has high importance: a reminder pops up and plays the phone's
 * notification sound. Do Not Disturb and the phone's sound settings still apply; ReadyShelf never
 * asks to bypass Do Not Disturb.
 */
public final class ReminderNotifier {

    /**
     * The notification channel, with high importance (it pops up and makes a sound). An app can't
     * make an existing channel louder, so this ID replaced the silent one on 2026-10-18. Its
     * importance can't change after it exists.
     */
    public static final String CHANNEL_ID = "review_alerts";
    /**
     * The silent channel used until 2026-10-18. createChannel() deletes it. Never use this ID
     * again: Android brings back a deleted channel's old settings when its ID is created again.
     */
    static final String OLD_SILENT_CHANNEL_ID = "review_reminders";
    /** The reminder's fixed ID. (The test reminder had 1002 until 2026-10-23.) */
    public static final int DAILY_ID = 1001;

    private static final String TAG = "ReminderNotifier";

    /** What one {@link #updateIfShown} did. */
    public enum UpdateResult {
        /** No reminder is shown (never shown, or removed), so nothing was posted. */
        NOT_SHOWN,
        /** The shown reminder already has these words and this number, so nothing was posted. */
        UNCHANGED,
        /** The shown reminder got the new words and number, without a pop-up or a sound. */
        UPDATED,
        /** Android refused the notification, or the reminder was removed just before. */
        NOT_UPDATED
    }

    private final Context context;

    public ReminderNotifier(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Creates the channel and deletes the old silent one. Doing it again changes nothing, so
     * this is safe at every start.
     */
    public void createChannel() {
        replaceChannel(context, CHANNEL_ID, OLD_SILENT_CHANNEL_ID);
    }

    /**
     * Creates the reminder channel {@code id} and deletes the channel {@code oldId}. If the
     * household had turned the old channel off in Android's settings, the new one starts off too.
     * The new channel is made before the old one is deleted, so an app start and a daily check
     * at the same moment can't lose that choice. Android never makes an existing channel louder,
     * so a later start can't undo a change the household made to the new channel. Tests call
     * this with their own IDs.
     */
    static void replaceChannel(@NonNull Context context, @NonNull String id,
                               @NonNull String oldId) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        NotificationChannelCompat old = manager.getNotificationChannelCompat(oldId);
        boolean oldTurnedOff = old != null
                && old.getImportance() == NotificationManagerCompat.IMPORTANCE_NONE;
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(id,
                oldTurnedOff ? NotificationManagerCompat.IMPORTANCE_NONE
                        : NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.reminder_channel_name))
                .setDescription(context.getString(R.string.reminder_channel_description))
                .build();
        manager.createNotificationChannel(channel);
        manager.deleteNotificationChannel(oldId);
    }

    /**
     * True when a reminder can appear: ReadyShelf's notifications and its reminder channel are
     * not turned off, and on Android 13 or newer the household allowed notifications.
     */
    public boolean canPost() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            return false;
        }
        NotificationChannelCompat channel = manager.getNotificationChannelCompat(CHANNEL_ID);
        if (channel != null && channel.getImportance() == NotificationManagerCompat.IMPORTANCE_NONE) {
            return false;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Shows the reminder with the items and the icon number of {@code list}. The daily check and
     * "Show reminder now" both use it. It pops up with a sound, also when it replaces a reminder
     * that is still shown. Returns false when it can't be shown.
     */
    public boolean showDaily(@NonNull ReminderList list) {
        return post(list, null);
    }

    /**
     * Gives the reminder that is still shown the words and icon number of {@code list} (since
     * 2026-10-23). It doesn't pop up or make a sound again, and it keeps the time it first came.
     * A reminder that isn't shown (never shown, or removed by the household) is not shown again,
     * and nothing is posted when nothing changed.
     */
    @NonNull
    public UpdateResult updateIfShown(@NonNull ReminderList list) {
        StatusBarNotification shown = shownReminder();
        if (shown == null) {
            return UpdateResult.NOT_SHOWN;
        }
        Notification old = shown.getNotification();
        if (old.number == list.getIconNumber() && TextUtils.equals(
                old.extras.getCharSequence(Notification.EXTRA_TEXT), listText(context, list))) {
            return UpdateResult.UNCHANGED;
        }
        return post(list, shown) ? UpdateResult.UPDATED : UpdateResult.NOT_UPDATED;
    }

    /** True while the reminder is in the phone's notification list. */
    public boolean isShown() {
        return shownReminder() != null;
    }

    /** Removes the reminder if it is still shown. */
    public void cancelDaily() {
        NotificationManagerCompat.from(context).cancel(DAILY_ID);
    }

    /**
     * The reminder's text for {@code list}: "Canned beans", "Canned beans and Granola bars",
     * or "Canned beans, Granola bars, and 3 others". The medicine entry is "medicine"
     * ("Medicine" when it comes first). An empty list gives the generic text.
     */
    @NonNull
    static String listText(@NonNull Context context, @NonNull ReminderList list) {
        List<ReminderList.Entry> shown = list.getShown();
        if (shown.isEmpty()) {
            return context.getString(R.string.reminder_daily_text);
        }
        String first = entryText(context, shown.get(0), true);
        if (shown.size() == 1) {
            return first;
        }
        String second = entryText(context, shown.get(1), false);
        int more = list.getMoreCount();
        if (more == 0) {
            return context.getString(R.string.reminder_daily_two, first, second);
        }
        return context.getResources().getQuantityString(R.plurals.reminder_daily_more, more,
                first, second, more);
    }

    @NonNull
    private static String entryText(@NonNull Context context, @NonNull ReminderList.Entry entry,
                                    boolean first) {
        if (!entry.isMedicine()) {
            return entry.getName();
        }
        return context.getString(first ? R.string.reminder_daily_medicine_first
                : R.string.reminder_daily_medicine);
    }

    /** The reminder as Android shows it now, or null when it isn't shown. */
    @Nullable
    private StatusBarNotification shownReminder() {
        try {
            for (StatusBarNotification shown
                    : NotificationManagerCompat.from(context).getActiveNotifications()) {
                if (shown.getId() == DAILY_ID && shown.getTag() == null) {
                    return shown;
                }
            }
        } catch (RuntimeException error) {
            // Android couldn't answer. Then the reminder counts as not shown, so nothing is
            // posted in its place.
            Log.w(TAG, "The shown notifications could not be read", error);
        }
        return null;
    }

    /**
     * Posts the reminder with the items and the icon number of {@code list}. Its lock-screen
     * (public) version has only the generic words. With {@code replacing} (the reminder that is
     * shown now) the post is an update: quiet, with the time of the shown one.
     */
    private boolean post(@NonNull ReminderList list, @Nullable StatusBarNotification replacing) {
        // Android 13 and newer: only with the household's permission. (Lint needs this check
        // in the same method as notify().)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (!canPost()) {
            return false;
        }
        createChannel();
        PendingIntent tap = openHome();
        // A locked phone that hides sensitive notification content shows this public version
        // instead. It never names items and has no number. When the phone shows all content on
        // the lock screen, the whole notification appears there: the household's setting decides.
        Notification publicVersion = builder(context.getString(R.string.reminder_daily_text), tap)
                .build();
        NotificationCompat.Builder reminder = builder(listText(context, list), tap)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                // The number for the app icon: Home's status tiles added up (see ReminderList).
                // Android doesn't show it in the notification itself.
                .setNumber(list.getIconNumber());
        if (replacing != null) {
            // An update: no second pop-up or sound (setSilent also stops the pop-up when the
            // first one never popped up, for example with the screen off), and the time stays.
            reminder.setOnlyAlertOnce(true)
                    .setSilent(true)
                    .setWhen(replacing.getNotification().when);
            // The household may have removed it in the meantime; then it stays removed. (A
            // removal at the very same moment can't be seen here; that post is quiet too.)
            if (!isShown()) {
                return false;
            }
        }
        try {
            NotificationManagerCompat.from(context).notify(DAILY_ID, reminder.build());
            return true;
        } catch (SecurityException notAllowed) {
            // The permission was taken away just now.
            Log.w(TAG, "The notification was not allowed", notAllowed);
            return false;
        }
    }

    @NonNull
    private NotificationCompat.Builder builder(@NonNull String words, @NonNull PendingIntent tap) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_daily_title))
                .setContentText(words)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(words))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(tap)
                .setAutoCancel(true)
                // Stays on the phone: not shown on a connected watch.
                .setLocalOnly(true);
    }

    /**
     * Opens Home at the top, with its banner (see HomeActivity.newReviewIntent). It points
     * straight at the screen, as Android requires. Each post gets its own, because each review
     * intent is new.
     */
    @NonNull
    private PendingIntent openHome() {
        return PendingIntent.getActivity(context, 0, HomeActivity.newReviewIntent(context),
                PendingIntent.FLAG_IMMUTABLE);
    }
}

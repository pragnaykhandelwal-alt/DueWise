package com.example.readyshelf.reminders;

import static com.example.readyshelf.reminders.NotificationChecks.assertStaysAbsent;
import static com.example.readyshelf.reminders.NotificationChecks.countShown;
import static com.example.readyshelf.reminders.NotificationChecks.removeReminders;
import static com.example.readyshelf.reminders.NotificationChecks.waitUntilGone;
import static com.example.readyshelf.reminders.NotificationChecks.waitUntilNumber;
import static com.example.readyshelf.reminders.NotificationChecks.waitUntilShown;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.BatchWithStatus;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The daily reminder from check to notification, on the emulator: at most one daily reminder per
 * local day, no second one after a restart, nothing while reminders are off, and the reminder is
 * removed when nothing needs review any more. Since 2026-10-19 it names up to two items (never
 * medicine) and counts the rest, while its lock-screen version stays generic. Since 2026-10-23
 * Settings' "Show reminder now" shows the same reminder at once, without recording it. Since
 * 2026-10-23 too, the reminder carries the number for the app icon (Home's tiles added up), and a
 * shown reminder is brought up to date quietly; a removed one stays removed. The
 * notifications are real; the batches and the policy are made up (the policy only returns preset
 * answers; it contains no date rules).
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReminderFlowTest {

    private static final String PREFS_FILE = "reminders-flow-test";
    private static final String WORK_NAME = "readyshelf-flow-test";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);
    private static final DateThresholds SETTINGS = DateThresholds.of(30, 180);

    // Made-up batches. The pain reliever's name must never appear in a notification.
    private static final BatchInfo PAIN_RELIEVER = new BatchInfo(1, "Pain reliever tablets",
            ItemCategory.MEDICINE, 10, "tablets", DAY.plusDays(3), DateLabel.EXPIRES, 3,
            "First aid box", LocationType.FIRST_AID, false);
    private static final BatchInfo BEANS = new BatchInfo(2, "Canned beans", ItemCategory.FOOD, 4,
            "cans", DAY.plusDays(90), DateLabel.BEST_BY, 2, "Earthquake kit",
            LocationType.EMERGENCY_KIT, false);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private ReminderNotifier notifier;
    private ReminderPrefs prefs;
    private ReminderScheduler scheduler;
    private FixedStatusPolicy policy;

    @Before
    public void setUp() {
        NotificationChecks.allowNotifications(context);
        notifier = new ReminderNotifier(context);
        notifier.createChannel();
        assertTrue("Turn on ReadyShelf's notifications on the emulator before this test",
                notifier.canPost());
        removeReminders(context);
        context.deleteSharedPreferences(PREFS_FILE);
        prefs = new ReminderPrefs(context, PREFS_FILE);
        prefs.setOn(true);
        scheduler = new ReminderScheduler(context, WORK_NAME,
                () -> ZonedDateTime.of(2026, 10, 11, 10, 0, 0, 0, ZoneId.systemDefault()));
        policy = new FixedStatusPolicy().status(PAIN_RELIEVER, DateStatus.NEAR_DATE);
    }

    @After
    public void tearDown() throws Exception {
        removeReminders(context);
        scheduler.cancel().getResult().get();
        context.deleteSharedPreferences(PREFS_FILE);
    }

    @Test
    public void twoChecksOnTheSameDay_showOneDailyReminder() {
        Reminders reminders = reminders(prefs);

        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        StatusBarNotification shown = waitUntilShown(context, ReminderNotifier.DAILY_ID);
        assertEquals(Reminders.DailyResult.ALREADY_SHOWN_TODAY, reminders.runDailyCheck(DAY));

        assertStaysAbsentSecondCopy();
        assertEquals(DAY, prefs.lastShownDay());
        // Every status came from the policy, for the day of the check.
        assertFalse(policy.days.isEmpty());
        for (LocalDate asked : policy.days) {
            assertEquals(DAY, asked);
        }

        Notification notification = shown.getNotification();
        assertEquals(ReminderNotifier.CHANNEL_ID, notification.getChannelId());
        // It pops up with a sound: its channel has high importance.
        NotificationChannelCompat channel = NotificationManagerCompat.from(context)
                .getNotificationChannelCompat(notification.getChannelId());
        assertNotNull(channel);
        assertEquals(NotificationManagerCompat.IMPORTANCE_HIGH, channel.getImportance());
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility);
        // Only the pain reliever causes the reminder. It is medicine, so it is never named.
        assertListText(notification, context.getString(R.string.reminder_daily_medicine_first));
        // The number for the app icon: Home's tiles added up (BEANS has a later date).
        assertEquals(1, notification.number);
        // A new reminder pops up as before: it isn't marked as a quiet update.
        assertEquals(0, notification.flags & Notification.FLAG_ONLY_ALERT_ONCE);
        assertNotNull("The lock screen needs a public version", notification.publicVersion);
        assertGeneric(notification.publicVersion);
    }

    @Test
    public void theReminder_namesTheFirstTwoItems_andCountsTheRest() {
        // Soonest date first: the beans' date passed, then the medicine, the bars, and the rice
        // are near. A second batch of beans adds no entry, and BEANS has a later date.
        BatchInfo passedBeans = foodBatch(11, "Canned beans", DAY.minusDays(2));
        BatchInfo bars = foodBatch(12, "Granola bars", DAY.plusDays(5));
        BatchInfo rice = foodBatch(13, "Rice", DAY.plusDays(10));
        BatchInfo moreBeans = foodBatch(14, "Canned beans", DAY.plusDays(12));
        policy = new FixedStatusPolicy()
                .status(passedBeans, DateStatus.PAST_DATE)
                .status(PAIN_RELIEVER, DateStatus.NEAR_DATE)
                .status(bars, DateStatus.NEAR_DATE)
                .status(rice, DateStatus.NEAR_DATE)
                .status(moreBeans, DateStatus.NEAR_DATE);
        Reminders reminders = reminders(prefs,
                Arrays.asList(rice, PAIN_RELIEVER, moreBeans, BEANS, bars, passedBeans));

        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));

        Notification notification = waitUntilShown(context, ReminderNotifier.DAILY_ID)
                .getNotification();
        assertListText(notification, "Canned beans, medicine, and 2 others");
        // Four names in the words, but five batches on Home's tiles: the second beans count too.
        assertEquals(5, notification.number);
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility);
        assertNotNull("The lock screen needs a public version", notification.publicVersion);
        assertGeneric(notification.publicVersion);
    }

    @Test
    public void afterARestartOnTheSameDay_noSecondDailyReminderIsShown() {
        assertEquals(Reminders.DailyResult.SHOWN, reminders(prefs).runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);
        // The household swipes it away; then the app starts again with new objects.
        removeReminders(context);

        Reminders afterRestart = reminders(new ReminderPrefs(context, PREFS_FILE));

        assertEquals(Reminders.DailyResult.ALREADY_SHOWN_TODAY, afterRestart.runDailyCheck(DAY));
        assertStaysAbsent(context, ReminderNotifier.DAILY_ID);
    }

    @Test
    public void onTheNextLocalDay_theReminderIsShownAgain() {
        Reminders reminders = reminders(prefs);
        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);
        removeReminders(context);

        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY.plusDays(1)));

        waitUntilShown(context, ReminderNotifier.DAILY_ID);
        assertEquals(DAY.plusDays(1), prefs.lastShownDay());
        assertTrue(policy.days.contains(DAY.plusDays(1)));
    }

    @Test
    public void remindersOff_showNothing() {
        prefs.setOn(false);

        assertEquals(Reminders.DailyResult.REMINDERS_OFF, reminders(prefs).runDailyCheck(DAY));

        assertStaysAbsent(context, ReminderNotifier.DAILY_ID);
        assertNull(prefs.lastShownDay());
    }

    @Test
    public void datesThatOnlyNeedReview_showNothing() {
        policy = new FixedStatusPolicy().status(PAIN_RELIEVER, DateStatus.NEEDS_REVIEW);

        assertEquals(Reminders.DailyResult.NOTHING_TO_REVIEW, reminders(prefs).runDailyCheck(DAY));

        assertStaysAbsent(context, ReminderNotifier.DAILY_ID);
    }

    @Test
    public void whenNothingNeedsReviewAnyMore_theShownReminderIsRemoved() {
        assertEquals(Reminders.DailyResult.SHOWN, reminders(prefs).runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);
        // For example, the medicine was recorded as used: no batch causes a reminder now.
        policy.status(PAIN_RELIEVER, DateStatus.LATER);

        assertEquals(Reminders.DailyResult.NOTHING_TO_REVIEW, reminders(prefs).runDailyCheck(DAY));

        waitUntilGone(context, ReminderNotifier.DAILY_ID);
    }

    @Test
    public void homesReviewList_removesTheReminderOnlyWhenNothingCausesIt() {
        Reminders reminders = reminders(prefs);
        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);

        reminders.onReviewListShown(Collections.singletonList(
                new BatchWithStatus(PAIN_RELIEVER, DateStatus.NEAR_DATE)));
        assertEquals(1, countShown(context, ReminderNotifier.DAILY_ID));

        reminders.onReviewListShown(Collections.singletonList(
                new BatchWithStatus(BEANS, DateStatus.NEEDS_REVIEW)));
        waitUntilGone(context, ReminderNotifier.DAILY_ID);
    }

    @Test
    public void turningOff_cancelsTheScheduleAndRemovesTheShownReminder() throws Exception {
        Reminders reminders = reminders(prefs);
        assertTrue(reminders.turnOn());
        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);

        reminders.turnOff();

        waitUntilGone(context, ReminderNotifier.DAILY_ID);
        assertFalse(prefs.isOn());
        // WorkManager saves the cancel on its own thread; wait for it, then check.
        scheduler.cancel().getResult().get();
        List<WorkInfo> work = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME).get();
        assertFalse(work.isEmpty());
        for (WorkInfo info : work) {
            assertEquals(WorkInfo.State.CANCELLED, info.getState());
        }
    }

    @Test
    public void showNow_namesTheItems_whileRemindersAreOff_andRecordsNothing() {
        prefs.setOn(false);
        // Soonest date first: the beans' date passed, then the bars, the medicine, and the rice
        // are near.
        BatchInfo passedBeans = foodBatch(11, "Canned beans", DAY.minusDays(2));
        BatchInfo bars = foodBatch(12, "Granola bars", DAY.plusDays(1));
        BatchInfo rice = foodBatch(13, "Rice", DAY.plusDays(10));
        policy = new FixedStatusPolicy()
                .status(passedBeans, DateStatus.PAST_DATE)
                .status(bars, DateStatus.NEAR_DATE)
                .status(PAIN_RELIEVER, DateStatus.NEAR_DATE)
                .status(rice, DateStatus.NEAR_DATE);
        Reminders reminders = reminders(prefs,
                Arrays.asList(rice, PAIN_RELIEVER, bars, passedBeans));

        assertEquals(Reminders.NowResult.SHOWN, reminders.showNow(DAY));

        Notification notification = waitUntilShown(context, ReminderNotifier.DAILY_ID)
                .getNotification();
        assertListText(notification, "Canned beans, Granola bars, and 2 others");
        assertEquals(4, notification.number);
        assertEquals(0, notification.flags & Notification.FLAG_ONLY_ALERT_ONCE);
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility);
        assertNotNull("The lock screen needs a public version", notification.publicVersion);
        assertGeneric(notification.publicVersion);
        // A second tap replaces it, and neither tap is recorded.
        assertEquals(Reminders.NowResult.SHOWN, reminders.showNow(DAY));
        assertStaysAbsentSecondCopy();
        assertNull(prefs.lastShownDay());
    }

    @Test
    public void showNow_isNotCounted_soTheDailyCheckStillShowsTodaysReminder() {
        Reminders reminders = reminders(prefs);
        assertEquals(Reminders.NowResult.SHOWN, reminders.showNow(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);

        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));

        assertEquals(DAY, prefs.lastShownDay());
        assertStaysAbsentSecondCopy();
    }

    @Test
    public void showNow_withNothingNearOrPassed_showsNothing_andRemovesAShownReminder() {
        Reminders reminders = reminders(prefs);
        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);
        // A date that only needs review never causes a reminder.
        policy.status(PAIN_RELIEVER, DateStatus.NEEDS_REVIEW);

        assertEquals(Reminders.NowResult.NOTHING_TO_REVIEW, reminders.showNow(DAY));

        waitUntilGone(context, ReminderNotifier.DAILY_ID);
        assertEquals(DAY, prefs.lastShownDay());
    }

    @Test
    public void showNow_withoutTheDateRules_orWhenTheyStop_showsNothing() {
        Reminders withoutRules = new Reminders(prefs, notifier, scheduler,
                () -> Collections.singletonList(PAIN_RELIEVER), () -> SETTINGS, listener -> { },
                null);
        Reminders stoppingRules = new Reminders(prefs, notifier, scheduler,
                () -> Collections.singletonList(PAIN_RELIEVER), () -> SETTINGS, listener -> { },
                (date, today, nearDateDays) -> {
                    throw new IllegalStateException("A made-up error");
                });

        assertEquals(Reminders.NowResult.RULES_NOT_READY, withoutRules.showNow(DAY));
        assertEquals(Reminders.NowResult.RULES_ERROR, stoppingRules.showNow(DAY));

        assertStaysAbsent(context, ReminderNotifier.DAILY_ID);
    }

    @Test
    public void showNowInBackground_givesTheResult() throws Exception {
        CountDownLatch answered = new CountDownLatch(1);
        AtomicReference<Reminders.NowResult> result = new AtomicReference<>();

        reminders(prefs).showNowInBackground(answer -> {
            result.set(answer);
            answered.countDown();
        });

        assertTrue("No answer within 5 seconds", answered.await(5, TimeUnit.SECONDS));
        assertEquals(Reminders.NowResult.SHOWN, result.get());
        waitUntilShown(context, ReminderNotifier.DAILY_ID);
    }

    // ----- The number for the app icon, and quiet updates (2026-10-23) -----

    @Test
    public void theIconNumber_countsLikeHomesTiles_notLikeTheWords() {
        // Home's tiles count the sample item and the empty batch too; the words leave them out.
        // A date that only needs review has no tile, and BEANS has a later date.
        BatchInfo sampleBars = new BatchInfo(21, "Sample bars", ItemCategory.FOOD, 6, "bars",
                DAY.minusDays(1), DateLabel.BEST_BY, 1, "Pantry", LocationType.PANTRY, true);
        BatchInfo emptySoup = new BatchInfo(22, "Empty soup", ItemCategory.FOOD, 0, "cans", DAY,
                DateLabel.BEST_BY, 1, "Pantry", LocationType.PANTRY, false);
        BatchInfo uncheckedRice = foodBatch(23, "Unchecked rice", DAY.plusDays(40));
        policy = new FixedStatusPolicy()
                .status(PAIN_RELIEVER, DateStatus.NEAR_DATE)
                .status(sampleBars, DateStatus.PAST_DATE)
                .status(emptySoup, DateStatus.TODAY)
                .status(uncheckedRice, DateStatus.NEEDS_REVIEW);
        Reminders reminders = reminders(prefs,
                Arrays.asList(PAIN_RELIEVER, BEANS, sampleBars, emptySoup, uncheckedRice));

        assertEquals(Reminders.NowResult.SHOWN, reminders.showNow(DAY));

        Notification notification = waitUntilShown(context, ReminderNotifier.DAILY_ID)
                .getNotification();
        assertListText(notification, context.getString(R.string.reminder_daily_medicine_first));
        assertEquals(3, notification.number);
        // The lock-screen version has no number.
        assertEquals(0, notification.publicVersion.number);
        assertGeneric(notification.publicVersion);
    }

    @Test
    public void homesList_bringsAShownReminderUpToDate_quietly() {
        Reminders reminders = reminders(prefs);
        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        Notification first = waitUntilNumber(context, ReminderNotifier.DAILY_ID, 1)
                .getNotification();
        // For example, the beans' date was corrected to one that passed.
        policy.status(BEANS, DateStatus.PAST_DATE);

        reminders.onReviewListShown(Arrays.asList(
                new BatchWithStatus(PAIN_RELIEVER, DateStatus.NEAR_DATE),
                new BatchWithStatus(BEANS, DateStatus.PAST_DATE)));

        Notification updated = waitUntilNumber(context, ReminderNotifier.DAILY_ID, 2)
                .getNotification();
        // Soonest date first: the medicine's date comes before the beans' date.
        assertListText(updated, "Medicine and Canned beans");
        assertEquals(1, countShown(context, ReminderNotifier.DAILY_ID));
        // Quiet: no second pop-up or sound, and the time it first came stays.
        assertTrue((updated.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
        assertEquals(Notification.GROUP_ALERT_SUMMARY, updated.getGroupAlertBehavior());
        assertEquals(first.when, updated.when);
        assertEquals(ReminderNotifier.CHANNEL_ID, updated.getChannelId());
        assertEquals(Notification.VISIBILITY_PRIVATE, updated.visibility);
        assertEquals(0, updated.publicVersion.number);
        assertGeneric(updated.publicVersion);
        // An update is not a new daily reminder.
        assertEquals(DAY, prefs.lastShownDay());
    }

    @Test
    public void savedChanges_updateTheShownReminder_thenRemoveIt() throws Exception {
        List<BatchInfo> batches = new CopyOnWriteArrayList<>(Arrays.asList(PAIN_RELIEVER, BEANS));
        AtomicReference<Runnable> changeListener = new AtomicReference<>();
        CountDownLatch listening = new CountDownLatch(1);
        Reminders reminders = new Reminders(prefs, notifier, scheduler, () -> batches,
                () -> SETTINGS, listener -> {
                    changeListener.set(listener);
                    listening.countDown();
                }, policy);
        reminders.onAppStart();
        assertTrue("onAppStart should start listening", listening.await(5, TimeUnit.SECONDS));
        assertEquals(Reminders.NowResult.SHOWN, reminders.showNow(DAY));
        waitUntilNumber(context, ReminderNotifier.DAILY_ID, 1);

        // A batch of granola bars with a near date is saved.
        BatchInfo bars = foodBatch(12, "Granola bars", DAY.plusDays(5));
        policy.status(bars, DateStatus.NEAR_DATE);
        batches.add(bars);
        changeListener.get().run();

        Notification updated = waitUntilNumber(context, ReminderNotifier.DAILY_ID, 2)
                .getNotification();
        assertListText(updated, "Medicine and Granola bars");
        assertTrue((updated.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);

        // Then both dates are corrected to later ones: nothing causes a reminder any more.
        policy.status(PAIN_RELIEVER, DateStatus.LATER).status(bars, DateStatus.LATER);
        changeListener.get().run();

        waitUntilGone(context, ReminderNotifier.DAILY_ID);
        assertNull(prefs.lastShownDay());
    }

    @Test
    public void aRemovedReminder_staysRemoved_andNothingIsRead() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicReference<Runnable> changeListener = new AtomicReference<>();
        CountDownLatch listening = new CountDownLatch(1);
        Reminders reminders = new Reminders(prefs, notifier, scheduler,
                () -> {
                    reads.incrementAndGet();
                    return Arrays.asList(PAIN_RELIEVER, BEANS);
                },
                () -> SETTINGS, listener -> {
                    changeListener.set(listener);
                    listening.countDown();
                }, policy);
        reminders.onAppStart();
        assertTrue("onAppStart should start listening", listening.await(5, TimeUnit.SECONDS));
        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);
        // The household swipes it away. Then the beans' date turns out to have passed.
        removeReminders(context);
        policy.status(BEANS, DateStatus.PAST_DATE);
        int readsBefore = reads.get();

        reminders.onReviewListShown(Arrays.asList(
                new BatchWithStatus(PAIN_RELIEVER, DateStatus.NEAR_DATE),
                new BatchWithStatus(BEANS, DateStatus.PAST_DATE)));
        changeListener.get().run();

        assertStaysAbsent(context, ReminderNotifier.DAILY_ID);
        assertEquals(Reminders.RefreshResult.NOTHING_SHOWN, reminders.refreshShown(DAY));
        assertEquals("Nothing is read while no reminder is shown", readsBefore, reads.get());
    }

    @Test
    public void aRefreshWithNothingNew_postsNothing() {
        Reminders reminders = reminders(prefs);
        assertEquals(Reminders.DailyResult.SHOWN, reminders.runDailyCheck(DAY));
        StatusBarNotification shown = waitUntilShown(context, ReminderNotifier.DAILY_ID);

        assertEquals(Reminders.RefreshResult.UNCHANGED, reminders.refreshShown(DAY));

        StatusBarNotification after = waitUntilShown(context, ReminderNotifier.DAILY_ID);
        assertEquals(shown.getPostTime(), after.getPostTime());
        assertEquals(0, after.getNotification().flags & Notification.FLAG_ONLY_ALERT_ONCE);
    }

    @Test
    public void aRefresh_withoutTheDateRules_orWhenTheyStop_keepsTheReminder() {
        assertEquals(Reminders.DailyResult.SHOWN, reminders(prefs).runDailyCheck(DAY));
        waitUntilShown(context, ReminderNotifier.DAILY_ID);
        Reminders withoutRules = new Reminders(prefs, notifier, scheduler,
                () -> Collections.singletonList(PAIN_RELIEVER), () -> SETTINGS, listener -> { },
                null);
        Reminders stoppingRules = new Reminders(prefs, notifier, scheduler,
                () -> Collections.singletonList(PAIN_RELIEVER), () -> SETTINGS, listener -> { },
                (date, today, nearDateDays) -> {
                    throw new IllegalStateException("A made-up error");
                });

        assertEquals(Reminders.RefreshResult.RULES_NOT_READY, withoutRules.refreshShown(DAY));
        assertEquals(Reminders.RefreshResult.RULES_ERROR, stoppingRules.refreshShown(DAY));

        assertStaysAbsentSecondCopy();
        assertEquals(1, waitUntilShown(context, ReminderNotifier.DAILY_ID)
                .getNotification().number);
    }

    // ----- Helpers -----

    @NonNull
    private Reminders reminders(@NonNull ReminderPrefs reminderPrefs) {
        return reminders(reminderPrefs, Arrays.asList(PAIN_RELIEVER, BEANS));
    }

    /** Reminders that never hear about saved changes (the tests above call it directly). */
    @NonNull
    private Reminders reminders(@NonNull ReminderPrefs reminderPrefs,
                                @NonNull List<BatchInfo> batches) {
        return new Reminders(reminderPrefs, notifier, scheduler, () -> batches, () -> SETTINGS,
                listener -> { }, policy);
    }

    /** A made-up food batch in a pantry. Each test batch needs its own date (FixedStatusPolicy). */
    @NonNull
    private static BatchInfo foodBatch(long id, @NonNull String name, @NonNull LocalDate date) {
        return new BatchInfo(id, name, ItemCategory.FOOD, 4, "units", date, DateLabel.BEST_BY, 1,
                "Pantry", LocationType.PANTRY, false);
    }

    private void assertStaysAbsentSecondCopy() {
        android.os.SystemClock.sleep(1000);
        assertEquals(1, countShown(context, ReminderNotifier.DAILY_ID));
    }

    /**
     * The notification has the reminder's title, and {@code expected} as its text, also when it
     * is expanded. The pain reliever is medicine, so its name never appears.
     */
    private void assertListText(@NonNull Notification notification, @NonNull String expected) {
        Bundle extras = notification.extras;
        String title = String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE));
        String text = String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT));
        String bigText = String.valueOf(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        assertEquals(context.getString(R.string.reminder_daily_title), title);
        assertEquals(expected, text);
        assertEquals(expected, bigText);
        for (String words : Arrays.asList(title, text, bigText)) {
            assertFalse(words, words.contains("Pain reliever"));
        }
    }

    /** The lock-screen version has only the generic words: no item names and no counts. */
    private void assertGeneric(@NonNull Notification notification) {
        Bundle extras = notification.extras;
        String title = String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE));
        String text = String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT));
        String bigText = String.valueOf(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        assertEquals(context.getString(R.string.reminder_daily_title), title);
        assertEquals(context.getString(R.string.reminder_daily_text), text);
        for (String words : Arrays.asList(title, text, bigText)) {
            assertFalse(words, words.contains("Pain reliever"));
            assertFalse(words, words.contains("Canned beans"));
            assertFalse(words, words.matches(".*\\d.*"));
        }
    }
}

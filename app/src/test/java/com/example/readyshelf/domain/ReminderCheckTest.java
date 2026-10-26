package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Tests for ReminderCheck: what one daily reminder check decides.
 *
 * <p>These tests don't test any date rules. The test policy below returns the status each test
 * sets up for a batch; it is not a DatePolicy.
 */
public class ReminderCheckTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);
    private static final DateThresholds THRESHOLDS = DateThresholds.of(14, 200);

    private static final BatchInfo BEANS = batch(1, ItemCategory.FOOD, 5, false);
    private static final BatchInfo PAIN_RELIEVER = batch(2, ItemCategory.MEDICINE, 10, false);
    private static final BatchInfo SAMPLE_BARS = batch(3, ItemCategory.FOOD, 6, true);
    private static final BatchInfo EMPTY_RICE = batch(4, ItemCategory.FOOD, 0, false);

    // ----- Off, and rules not connected: nothing is read -----

    @Test
    public void remindersOff_readsNothing() {
        CountingBatches batches = new CountingBatches(BEANS);

        ReminderCheck.Outcome outcome = ReminderCheck.decide(false,
                new PresetPolicy().status(BEANS, DateStatus.PAST_DATE), true, null, TODAY,
                batches, THRESHOLDS);

        assertEquals(ReminderCheck.Outcome.REMINDERS_OFF, outcome);
        assertEquals(0, batches.reads);
    }

    @Test
    public void rulesNotConnected_readsNothing() {
        CountingBatches batches = new CountingBatches(BEANS);

        ReminderCheck.Outcome outcome = ReminderCheck.decide(true, null, true, null, TODAY,
                batches, THRESHOLDS);

        assertEquals(ReminderCheck.Outcome.RULES_NOT_READY, outcome);
        assertEquals(0, batches.reads);
    }

    // ----- Which statuses cause a reminder -----

    @Test
    public void datePassed_today_andNearDate_eachCauseAReminder() {
        for (DateStatus status : Arrays.asList(DateStatus.PAST_DATE, DateStatus.TODAY,
                DateStatus.NEAR_DATE)) {
            PresetPolicy policy = new PresetPolicy().status(BEANS, status);

            assertEquals(status.name(), ReminderCheck.Outcome.SHOW, decide(policy, null, BEANS));
        }
    }

    @Test
    public void medicineWithANearDate_causesAReminderToo() {
        PresetPolicy policy = new PresetPolicy().status(PAIN_RELIEVER, DateStatus.NEAR_DATE);

        assertEquals(ReminderCheck.Outcome.SHOW, decide(policy, null, PAIN_RELIEVER));
    }

    @Test
    public void laterDates_andDatesThatNeedReview_causeNoReminder() {
        PresetPolicy policy = new PresetPolicy()
                .status(BEANS, DateStatus.LATER)
                .status(PAIN_RELIEVER, DateStatus.NEEDS_REVIEW);

        assertEquals(ReminderCheck.Outcome.NOTHING_TO_REVIEW,
                decide(policy, null, BEANS, PAIN_RELIEVER));
    }

    @Test
    public void sampleItems_andBatchesWithNothingLeft_causeNoReminder() {
        PresetPolicy policy = new PresetPolicy()
                .status(SAMPLE_BARS, DateStatus.PAST_DATE)
                .status(EMPTY_RICE, DateStatus.TODAY);

        assertEquals(ReminderCheck.Outcome.NOTHING_TO_REVIEW,
                decide(policy, null, SAMPLE_BARS, EMPTY_RICE));
    }

    @Test
    public void noBatches_causeNoReminder() {
        assertEquals(ReminderCheck.Outcome.NOTHING_TO_REVIEW, decide(new PresetPolicy(), null));
    }

    // ----- At most one daily reminder per local day -----

    @Test
    public void alreadyShownToday_isNotShownAgain() {
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.NEAR_DATE);

        assertEquals(ReminderCheck.Outcome.ALREADY_SHOWN_TODAY, decide(policy, TODAY, BEANS));
    }

    @Test
    public void shownYesterday_isShownAgainOnANewLocalDay() {
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.NEAR_DATE);

        assertEquals(ReminderCheck.Outcome.SHOW, decide(policy, TODAY.minusDays(1), BEANS));
    }

    @Test
    public void aLastDayAfterToday_doesNotBlockReminders() {
        // For example, after the phone's clock was once set a year ahead by mistake.
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.NEAR_DATE);

        assertEquals(ReminderCheck.Outcome.SHOW, decide(policy, TODAY.plusYears(1), BEANS));
    }

    @Test
    public void nothingToReview_isFoundEvenWhenTodaysReminderWasShown() {
        // So a reminder that no longer applies can be removed later the same day.
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.LATER);

        assertEquals(ReminderCheck.Outcome.NOTHING_TO_REVIEW, decide(policy, TODAY, BEANS));
    }

    @Test
    public void notificationsNotAllowed_isReportedInsteadOfShowing() {
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.TODAY);

        ReminderCheck.Outcome outcome = ReminderCheck.decide(true, policy, false, null, TODAY,
                new CountingBatches(BEANS), THRESHOLDS);

        assertEquals(ReminderCheck.Outcome.NOTIFICATIONS_BLOCKED, outcome);
    }

    // ----- What the date rules receive -----

    @Test
    public void thePolicyGetsTheCheckDate_andTheNearDateDays() {
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.LATER);

        ReminderCheck.decide(true, policy, true, null, TODAY, new CountingBatches(BEANS),
                THRESHOLDS);

        assertEquals(Collections.singletonList(TODAY), policy.days);
        assertEquals(Collections.singletonList(14), policy.nearDateDays);
    }

    @Test
    public void anErrorInTheRules_reachesTheCaller() {
        PresetPolicy policy = new PresetPolicy(); // no status set, so statusOf returns null

        assertThrows(IllegalStateException.class, () -> decide(policy, null, BEANS));
    }

    // ----- The items the reminder lists (added 2026-10-19) -----

    @Test
    public void check_givesTheItemsThatCauseTheReminder_whenItIsShown() {
        PresetPolicy policy = new PresetPolicy()
                .status(BEANS, DateStatus.PAST_DATE)
                .status(PAIN_RELIEVER, DateStatus.NEAR_DATE)
                .status(SAMPLE_BARS, DateStatus.PAST_DATE);

        ReminderCheck.Decision decision = ReminderCheck.check(true, policy, true, null, TODAY,
                new CountingBatches(SAMPLE_BARS, PAIN_RELIEVER, BEANS), THRESHOLDS);

        assertEquals(ReminderCheck.Outcome.SHOW, decision.getOutcome());
        // Soonest date first (the beans' date comes first), the medicine without its name, and
        // no sample item.
        assertEquals(Arrays.asList(ReminderList.Entry.named("Item 1"),
                ReminderList.Entry.MEDICINE), decision.getList().getShown());
        assertEquals(0, decision.getList().getMoreCount());
        // The number for the app icon counts like Home's tiles, the sample item too (2026-10-23).
        assertEquals(3, decision.getList().getIconNumber());
    }

    @Test
    public void check_givesNoItems_whenNoReminderIsShown() {
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.NEAR_DATE);

        ReminderCheck.Decision off = ReminderCheck.check(false, policy, true, null, TODAY,
                new CountingBatches(BEANS), THRESHOLDS);
        ReminderCheck.Decision shownToday = ReminderCheck.check(true, policy, true, TODAY, TODAY,
                new CountingBatches(BEANS), THRESHOLDS);
        ReminderCheck.Decision blocked = ReminderCheck.check(true, policy, false, null, TODAY,
                new CountingBatches(BEANS), THRESHOLDS);

        assertEquals(ReminderCheck.Outcome.REMINDERS_OFF, off.getOutcome());
        assertEquals(ReminderCheck.Outcome.ALREADY_SHOWN_TODAY, shownToday.getOutcome());
        assertEquals(ReminderCheck.Outcome.NOTIFICATIONS_BLOCKED, blocked.getOutcome());
        for (ReminderCheck.Decision decision : Arrays.asList(off, shownToday, blocked)) {
            assertEquals(ReminderList.EMPTY, decision.getList());
        }
    }

    // ----- The shared filter that Home uses too -----

    @Test
    public void anyCausesReminder_looksAtStatusSampleAndQuantity() {
        assertTrue(ReminderCheck.anyCausesReminder(Arrays.asList(
                new BatchWithStatus(BEANS, DateStatus.LATER),
                new BatchWithStatus(PAIN_RELIEVER, DateStatus.PAST_DATE))));
        assertFalse(ReminderCheck.anyCausesReminder(Arrays.asList(
                new BatchWithStatus(SAMPLE_BARS, DateStatus.PAST_DATE),
                new BatchWithStatus(EMPTY_RICE, DateStatus.TODAY),
                new BatchWithStatus(BEANS, DateStatus.NEEDS_REVIEW))));
        assertFalse(ReminderCheck.anyCausesReminder(Collections.emptyList()));
    }

    // ----- Test helpers -----

    private static ReminderCheck.Outcome decide(PresetPolicy policy, LocalDate lastShownDay,
                                                BatchInfo... batches) {
        return ReminderCheck.decide(true, policy, true, lastShownDay, TODAY,
                new CountingBatches(batches), THRESHOLDS);
    }

    private static BatchInfo batch(long id, ItemCategory category, int quantity, boolean sample) {
        // Each batch gets its own date, so the preset policy can tell them apart.
        return new BatchInfo(id, "Item " + id, category, quantity, "units",
                LocalDate.of(2026, 10, 1).plusDays(id), DateLabel.BEST_BY, 2, "Earthquake kit",
                LocationType.EMERGENCY_KIT, sample);
    }

    /** Gives the same batches each time, and counts how often they were read. */
    private static final class CountingBatches implements Supplier<List<BatchInfo>> {
        private final List<BatchInfo> batches;
        private int reads;

        CountingBatches(BatchInfo... batches) {
            this.batches = Arrays.asList(batches);
        }

        @Override
        public List<BatchInfo> get() {
            reads++;
            return batches;
        }
    }

    /**
     * Returns the status set up for each batch's date input, and remembers what it was given.
     * Not a DatePolicy: it contains no date rules.
     */
    private static final class PresetPolicy implements DatePolicyContract {
        private final Map<DateInput, DateStatus> statuses = new HashMap<>();
        private final List<LocalDate> days = new ArrayList<>();
        private final List<Integer> nearDateDays = new ArrayList<>();

        PresetPolicy status(BatchInfo batch, DateStatus status) {
            statuses.put(DateInput.from(batch), status);
            return this;
        }

        @Override
        public DateStatus statusOf(DateInput date, LocalDate today, int nearDays) {
            days.add(today);
            nearDateDays.add(nearDays);
            return statuses.get(date);
        }
    }
}

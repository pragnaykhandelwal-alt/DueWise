package com.example.readyshelf.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateInput;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;

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
 * Tests for ReviewWidgetState: what the home-screen widget says, and (since 2026-10-23) its
 * number, Home's tiles added up.
 *
 * <p>These tests don't test any date rules. The test policy below returns the status each test
 * sets up for a batch; it is not a DatePolicy.
 */
public class ReviewWidgetStateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private static final DateThresholds THRESHOLDS = DateThresholds.of(14, 200);

    private static final BatchInfo BEANS = batch(1, ItemCategory.FOOD, 5, false);
    private static final BatchInfo RICE = batch(2, ItemCategory.FOOD, 3, false);
    private static final BatchInfo PAIN_RELIEVER = batch(3, ItemCategory.MEDICINE, 10, false);
    private static final BatchInfo OLD_SAMPLE_BARS = batch(4, ItemCategory.FOOD, 6, true);
    private static final BatchInfo EMPTY_SOUP = batch(5, ItemCategory.FOOD, 0, false);

    @Test
    public void rulesNotConnected_isUnknown_andReadsNothing() {
        CountingBatches batches = new CountingBatches(BEANS);

        assertEquals(ReviewWidgetState.UNKNOWN,
                ReviewWidgetState.check(null, batches, TODAY, THRESHOLDS));
        assertEquals(0, batches.reads);
    }

    @Test
    public void noBatches_meanNothingToReview() {
        assertEquals(ReviewWidgetState.NOTHING_TO_REVIEW, check(new PresetPolicy()));
    }

    @Test
    public void onlyLaterDates_meanNothingToReview() {
        PresetPolicy policy = new PresetPolicy()
                .status(BEANS, DateStatus.LATER)
                .status(RICE, DateStatus.LATER);

        assertEquals(ReviewWidgetState.NOTHING_TO_REVIEW, check(policy, BEANS, RICE));
    }

    @Test
    public void everyStatusButLater_meansItemsToReview() {
        for (DateStatus status : Arrays.asList(DateStatus.NEEDS_REVIEW, DateStatus.PAST_DATE,
                DateStatus.TODAY, DateStatus.NEAR_DATE)) {
            PresetPolicy policy = new PresetPolicy().status(BEANS, status);

            assertEquals(status.name(), ReviewWidgetState.Kind.ITEMS_TO_REVIEW,
                    check(policy, BEANS).getKind());
        }
    }

    @Test
    public void theNumber_countsOnlyStatusesWithATileOnHome() {
        // A date to review gets no tile on Home, so it adds nothing to the number.
        assertEquals(ReviewWidgetState.itemsToReview(0),
                check(new PresetPolicy().status(BEANS, DateStatus.NEEDS_REVIEW), BEANS));
        for (DateStatus status : Arrays.asList(DateStatus.PAST_DATE, DateStatus.TODAY,
                DateStatus.NEAR_DATE)) {
            PresetPolicy policy = new PresetPolicy().status(BEANS, status);

            assertEquals(status.name(), ReviewWidgetState.itemsToReview(1), check(policy, BEANS));
        }
    }

    @Test
    public void theNumber_isHomesTilesAddedUp() {
        PresetPolicy policy = new PresetPolicy()
                .status(BEANS, DateStatus.PAST_DATE)
                .status(RICE, DateStatus.NEAR_DATE)
                .status(PAIN_RELIEVER, DateStatus.TODAY)
                .status(OLD_SAMPLE_BARS, DateStatus.NEEDS_REVIEW)
                .status(EMPTY_SOUP, DateStatus.NEAR_DATE);

        // Tiles: Date passed 1, Date is today 1, Near date 2. The sample bars only need a date.
        assertEquals(ReviewWidgetState.itemsToReview(4),
                check(policy, BEANS, RICE, PAIN_RELIEVER, OLD_SAMPLE_BARS, EMPTY_SOUP));
    }

    @Test
    public void medicine_oldSampleItems_andEmptyBatches_countLikeOnHome() {
        // Home lists them, so the widget counts them. Only the daily reminder leaves the sample
        // item and the empty batch out (ReminderCheck).
        for (BatchInfo batch : Arrays.asList(PAIN_RELIEVER, OLD_SAMPLE_BARS, EMPTY_SOUP)) {
            PresetPolicy policy = new PresetPolicy().status(batch, DateStatus.PAST_DATE);

            assertEquals(batch.getName(), ReviewWidgetState.itemsToReview(1),
                    check(policy, batch));
        }
    }

    @Test
    public void oneItemAmongLaterDates_isEnough() {
        PresetPolicy policy = new PresetPolicy()
                .status(BEANS, DateStatus.LATER)
                .status(RICE, DateStatus.NEAR_DATE)
                .status(PAIN_RELIEVER, DateStatus.LATER);

        assertEquals(ReviewWidgetState.itemsToReview(1),
                check(policy, BEANS, RICE, PAIN_RELIEVER));
    }

    @Test
    public void statesAreEqual_onlyWithTheSameKindAndNumber() {
        assertEquals(ReviewWidgetState.itemsToReview(2), ReviewWidgetState.itemsToReview(2));
        assertEquals(ReviewWidgetState.itemsToReview(2).hashCode(),
                ReviewWidgetState.itemsToReview(2).hashCode());
        assertNotEquals(ReviewWidgetState.itemsToReview(2), ReviewWidgetState.itemsToReview(3));
        assertNotEquals(ReviewWidgetState.itemsToReview(0), ReviewWidgetState.NOTHING_TO_REVIEW);
        assertNotEquals(ReviewWidgetState.UNKNOWN, ReviewWidgetState.NOTHING_TO_REVIEW);
        assertEquals(ReviewWidgetState.Kind.UNKNOWN, ReviewWidgetState.UNKNOWN.getKind());
        assertEquals(0, ReviewWidgetState.UNKNOWN.getNumber());
        assertEquals(0, ReviewWidgetState.NOTHING_TO_REVIEW.getNumber());
    }

    @Test
    public void thePolicyGetsTheDay_andTheNearDateDays() {
        PresetPolicy policy = new PresetPolicy().status(BEANS, DateStatus.LATER);

        check(policy, BEANS);

        assertEquals(Collections.singletonList(TODAY), policy.days);
        assertEquals(Collections.singletonList(14), policy.nearDateDays);
    }

    @Test
    public void anErrorInTheRules_reachesTheCaller() {
        PresetPolicy policy = new PresetPolicy(); // no status set, so statusOf returns null

        assertThrows(IllegalStateException.class, () -> check(policy, BEANS));
    }

    // ----- Test helpers -----

    private static ReviewWidgetState check(PresetPolicy policy, BatchInfo... batches) {
        return ReviewWidgetState.check(policy, new CountingBatches(batches), TODAY, THRESHOLDS);
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

package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for ReviewCounts: the counts in the "Items need review" banner at the top of Home, and
 * (since 2026-10-23) Home's tiles added up, the number on the app icon and the widget.
 *
 * <p>These tests don't test any date rules. Each item gets the status that the test sets up; the
 * test policy below is not a DatePolicy.
 */
public class ReviewCountsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private static final DateThresholds THRESHOLDS = DateThresholds.of(30, 180);

    private static final BatchInfo BEANS = batch(1, ItemCategory.FOOD, 5, false);
    private static final BatchInfo RICE = batch(2, ItemCategory.FOOD, 3, false);
    private static final BatchInfo WATER = batch(3, ItemCategory.WATER, 12, false);
    private static final BatchInfo PAIN_RELIEVER = batch(4, ItemCategory.MEDICINE, 10, false);
    private static final BatchInfo SAMPLE_BARS = batch(5, ItemCategory.FOOD, 6, true);
    private static final BatchInfo EMPTY_SOUP = batch(6, ItemCategory.FOOD, 0, false);

    @Test
    public void anEmptyReviewList_givesNoCounts() {
        assertTrue(ReviewCounts.byStatus(Collections.emptyList()).isEmpty());
    }

    @Test
    public void eachStatus_getsOneEntry_inTheBannersOrder() {
        List<ReviewCounts.StatusCount> counts = ReviewCounts.byStatus(Arrays.asList(
                item(BEANS, DateStatus.NEEDS_REVIEW),
                item(RICE, DateStatus.NEAR_DATE),
                item(WATER, DateStatus.TODAY),
                item(PAIN_RELIEVER, DateStatus.PAST_DATE)));

        assertEquals(Arrays.asList(
                count(DateStatus.NEAR_DATE, 1),
                count(DateStatus.TODAY, 1),
                count(DateStatus.PAST_DATE, 1),
                count(DateStatus.NEEDS_REVIEW, 1)), counts);
    }

    @Test
    public void batchesWithTheSameStatus_areAddedUp() {
        List<ReviewCounts.StatusCount> counts = ReviewCounts.byStatus(Arrays.asList(
                item(BEANS, DateStatus.NEAR_DATE),
                item(RICE, DateStatus.NEEDS_REVIEW),
                item(WATER, DateStatus.NEAR_DATE),
                item(PAIN_RELIEVER, DateStatus.PAST_DATE),
                item(SAMPLE_BARS, DateStatus.NEAR_DATE),
                item(EMPTY_SOUP, DateStatus.NEEDS_REVIEW)));

        assertEquals(Arrays.asList(
                count(DateStatus.NEAR_DATE, 3),
                count(DateStatus.PAST_DATE, 1),
                count(DateStatus.NEEDS_REVIEW, 2)), counts);
    }

    @Test
    public void aStatusThatNoBatchHas_getsNoEntry() {
        List<ReviewCounts.StatusCount> counts = ReviewCounts.byStatus(Arrays.asList(
                item(BEANS, DateStatus.TODAY),
                item(RICE, DateStatus.TODAY)));

        assertEquals(Collections.singletonList(count(DateStatus.TODAY, 2)), counts);
    }

    @Test
    public void medicineSampleItemsAndBatchesWithNothingLeft_areCountedToo() {
        // Home lists them, so the banner counts them. Only the daily reminder leaves the sample
        // item and the empty batch out (ReminderCheck).
        List<ReviewCounts.StatusCount> counts = ReviewCounts.byStatus(Arrays.asList(
                item(PAIN_RELIEVER, DateStatus.PAST_DATE),
                item(SAMPLE_BARS, DateStatus.PAST_DATE),
                item(EMPTY_SOUP, DateStatus.PAST_DATE)));

        assertEquals(Collections.singletonList(count(DateStatus.PAST_DATE, 3)), counts);
    }

    @Test
    public void aLaterItem_isNotCounted() {
        // RuleRunner.needsReview never gives one, but a later date must not show in the banner.
        assertTrue(ReviewCounts.byStatus(Collections.singletonList(
                item(BEANS, DateStatus.LATER))).isEmpty());
    }

    @Test
    public void theCounts_matchEveryRowOfHomesReviewLists() {
        PresetPolicy policy = new PresetPolicy()
                .status(BEANS, DateStatus.NEAR_DATE)
                .status(RICE, DateStatus.LATER)
                .status(WATER, DateStatus.NEEDS_REVIEW)
                .status(PAIN_RELIEVER, DateStatus.PAST_DATE)
                .status(SAMPLE_BARS, DateStatus.NEAR_DATE);
        List<BatchWithStatus> reviewList = RuleRunner.needsReview(
                Arrays.asList(BEANS, RICE, WATER, PAIN_RELIEVER, SAMPLE_BARS), policy, TODAY,
                THRESHOLDS);

        List<ReviewCounts.StatusCount> counts = ReviewCounts.byStatus(reviewList);

        assertEquals(Arrays.asList(
                count(DateStatus.NEAR_DATE, 2),
                count(DateStatus.PAST_DATE, 1),
                count(DateStatus.NEEDS_REVIEW, 1)), counts);
        int total = 0;
        for (ReviewCounts.StatusCount statusCount : counts) {
            total += statusCount.getCount();
        }
        assertEquals(reviewList.size(), total);
    }

    @Test
    public void theResult_cannotBeChanged() {
        List<ReviewCounts.StatusCount> counts = ReviewCounts.byStatus(Collections.singletonList(
                item(BEANS, DateStatus.TODAY)));

        assertThrows(UnsupportedOperationException.class,
                () -> counts.add(count(DateStatus.NEAR_DATE, 1)));
    }

    // ----- Home's tiles added up: the number on the app icon and the widget (2026-10-23) -----

    @Test
    public void onlyDatePassed_today_andNearDate_haveATile() {
        assertTrue(ReviewCounts.hasTile(DateStatus.PAST_DATE));
        assertTrue(ReviewCounts.hasTile(DateStatus.TODAY));
        assertTrue(ReviewCounts.hasTile(DateStatus.NEAR_DATE));
        assertFalse(ReviewCounts.hasTile(DateStatus.NEEDS_REVIEW));
        assertFalse(ReviewCounts.hasTile(DateStatus.LATER));
    }

    @Test
    public void anEmptyReviewList_hasATotalOfZero() {
        assertEquals(0, ReviewCounts.tilesTotal(Collections.emptyList()));
    }

    @Test
    public void theTotal_leavesOutDatesToReview_andLaterDates() {
        assertEquals(3, ReviewCounts.tilesTotal(Arrays.asList(
                item(BEANS, DateStatus.NEEDS_REVIEW),
                item(RICE, DateStatus.NEAR_DATE),
                item(WATER, DateStatus.TODAY),
                item(PAIN_RELIEVER, DateStatus.PAST_DATE),
                item(SAMPLE_BARS, DateStatus.LATER))));
    }

    @Test
    public void theTotal_countsMedicineSampleItemsAndBatchesWithNothingLeft() {
        // Like the tiles. Only the reminder's words leave the sample item and the empty batch out.
        assertEquals(3, ReviewCounts.tilesTotal(Arrays.asList(
                item(PAIN_RELIEVER, DateStatus.PAST_DATE),
                item(SAMPLE_BARS, DateStatus.NEAR_DATE),
                item(EMPTY_SOUP, DateStatus.TODAY))));
    }

    @Test
    public void theTotal_isTheTilesHomeShows_addedUp() {
        PresetPolicy policy = new PresetPolicy()
                .status(BEANS, DateStatus.NEAR_DATE)
                .status(RICE, DateStatus.LATER)
                .status(WATER, DateStatus.NEEDS_REVIEW)
                .status(PAIN_RELIEVER, DateStatus.PAST_DATE)
                .status(SAMPLE_BARS, DateStatus.NEAR_DATE);
        List<BatchWithStatus> reviewList = RuleRunner.needsReview(
                Arrays.asList(BEANS, RICE, WATER, PAIN_RELIEVER, SAMPLE_BARS), policy, TODAY,
                THRESHOLDS);

        // Home's tiles: every count except "Date needs review" (HomeActivity.showTiles).
        int tiles = 0;
        for (ReviewCounts.StatusCount statusCount : ReviewCounts.byStatus(reviewList)) {
            if (ReviewCounts.hasTile(statusCount.getStatus())) {
                tiles += statusCount.getCount();
            }
        }
        assertEquals(4, reviewList.size());
        assertEquals(3, tiles);
        assertEquals(tiles, ReviewCounts.tilesTotal(reviewList));
    }

    // ----- Test helpers -----

    private static BatchWithStatus item(BatchInfo batch, DateStatus status) {
        return new BatchWithStatus(batch, status);
    }

    private static ReviewCounts.StatusCount count(DateStatus status, int count) {
        return new ReviewCounts.StatusCount(status, count);
    }

    private static BatchInfo batch(long id, ItemCategory category, int quantity, boolean sample) {
        // Each batch gets its own date, so the preset policy can tell them apart.
        return new BatchInfo(id, "Item " + id, category, quantity, "units",
                LocalDate.of(2026, 10, 1).plusDays(id), DateLabel.BEST_BY, 2, "Earthquake kit",
                LocationType.EMERGENCY_KIT, sample);
    }

    /**
     * Returns the status set up for each batch's date input. Not a DatePolicy: it contains no
     * date rules.
     */
    private static final class PresetPolicy implements DatePolicyContract {
        private final Map<DateInput, DateStatus> statuses = new HashMap<>();

        PresetPolicy status(BatchInfo batch, DateStatus status) {
            statuses.put(DateInput.from(batch), status);
            return this;
        }

        @Override
        public DateStatus statusOf(DateInput date, LocalDate today, int nearDays) {
            return statuses.get(date);
        }
    }
}

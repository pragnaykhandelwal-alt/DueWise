package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for RenewalList: which items the Renewal section of the Purchase/Renewal list shows
 * (added 2026-10-24).
 *
 * <p>These tests don't test any date rules: each item gets its status here, the way the date
 * rules would give it, and the review lists are already in Home's order.
 */
public class RenewalListTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 24);

    @Test
    public void documentsAndSubscriptions_datedTodayOrEarlier_areListed() {
        BatchWithStatus passport = item(1, "Passport", ItemCategory.DOCUMENT, 1,
                DateStatus.PAST_DATE);
        BatchWithStatus streaming = item(2, "Streaming plan", ItemCategory.SUBSCRIPTION, 1,
                DateStatus.TODAY);

        assertEquals(Arrays.asList(passport, streaming),
                RenewalList.of(Arrays.asList(passport, streaming)));
    }

    @Test
    public void otherCategories_areNeverListed() {
        for (ItemCategory category : ItemCategory.values()) {
            boolean renewed = category == ItemCategory.DOCUMENT
                    || category == ItemCategory.SUBSCRIPTION;
            assertEquals(category.name(), renewed, RenewalList.isRenewed(category));
            if (!renewed) {
                assertEquals(category.name(), Collections.emptyList(), RenewalList.of(
                        Collections.singletonList(item(1, "Item", category, 3,
                                DateStatus.PAST_DATE))));
            }
        }
    }

    @Test
    public void nearDateLaterDate_andDatesThatNeedReview_areNotListed() {
        List<BatchWithStatus> reviewList = Arrays.asList(
                item(1, "Passport", ItemCategory.DOCUMENT, 1, DateStatus.NEAR_DATE),
                item(2, "Visa", ItemCategory.DOCUMENT, 1, DateStatus.LATER),
                item(3, "Streaming plan", ItemCategory.SUBSCRIPTION, 1,
                        DateStatus.NEEDS_REVIEW));

        assertEquals(Collections.emptyList(), RenewalList.of(reviewList));
    }

    @Test
    public void aBatchWithNothingLeft_isNotListed() {
        BatchWithStatus nothingLeft = item(1, "Passport", ItemCategory.DOCUMENT, 0,
                DateStatus.PAST_DATE);

        assertEquals(Collections.emptyList(),
                RenewalList.of(Collections.singletonList(nothingLeft)));
    }

    @Test
    public void sampleItems_areListedLikeOtherItems() {
        BatchWithStatus sample = new BatchWithStatus(new BatchInfo(1, "Sample visa",
                ItemCategory.DOCUMENT, 1, "document", DAY.minusDays(1), DateLabel.EXPIRES, 1,
                "Pantry", LocationType.PANTRY, true), DateStatus.PAST_DATE);

        assertEquals(Collections.singletonList(sample),
                RenewalList.of(Collections.singletonList(sample)));
    }

    @Test
    public void theReviewListsOrder_isKept_andTheResultCantBeChanged() {
        BatchWithStatus visa = item(1, "Visa", ItemCategory.DOCUMENT, 1, DateStatus.PAST_DATE);
        BatchWithStatus beans = item(2, "Canned beans", ItemCategory.FOOD, 2,
                DateStatus.PAST_DATE);
        BatchWithStatus license = item(3, "Driver's license", ItemCategory.DOCUMENT, 1,
                DateStatus.PAST_DATE);
        BatchWithStatus streaming = item(4, "Streaming plan", ItemCategory.SUBSCRIPTION, 1,
                DateStatus.TODAY);

        List<BatchWithStatus> renewals = RenewalList.of(
                Arrays.asList(visa, beans, license, streaming));

        assertEquals(Arrays.asList(visa, license, streaming), renewals);
        assertThrows(UnsupportedOperationException.class, () -> renewals.add(visa));
        assertTrue(RenewalList.of(Collections.emptyList()).isEmpty());
    }

    private static BatchWithStatus item(long id, String name, ItemCategory category,
                                        int quantity, DateStatus status) {
        return new BatchWithStatus(new BatchInfo(id, name, category, quantity, "units",
                DAY.minusDays(id), DateLabel.EXPIRES, 1, "Pantry", LocationType.PANTRY, false),
                status);
    }
}

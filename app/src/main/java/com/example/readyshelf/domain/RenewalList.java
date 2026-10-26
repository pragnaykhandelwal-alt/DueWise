package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Picks the items for the Renewal section of the Purchase/Renewal list (added 2026-10-24, the
 * student's request and choices). This is plain Java, so it can be tested without a phone.
 *
 * <p>An item is listed when all of these are true:
 * <ul>
 *   <li>it is a document or a subscription: these are renewed, not bought, so they never get a
 *       replacement need (see RuleRunner);</li>
 *   <li>the date rules (DatePolicy) give it "Date is today" or "Date passed", the statuses of
 *       Home's tiles for today or earlier. A date that needs review is not listed;</li>
 *   <li>something is left of it (quantity 1 or more), like the replacement needs and the daily
 *       reminder. A batch with nothing left was already recorded as used or discarded.</li>
 * </ul>
 * Archived batches never reach this class: the date rules never see them. A category added
 * later stays out until it is named here.
 */
public final class RenewalList {

    // Documents and subscriptions (the student's choice on 2026-10-24).
    private static final Set<ItemCategory> RENEWED = EnumSet.of(ItemCategory.DOCUMENT,
            ItemCategory.SUBSCRIPTION);

    private RenewalList() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * The items of a review list from {@link RuleRunner#needsReview} that belong under Renewal,
     * in the same order: soonest date first. The result can't be changed.
     */
    @NonNull
    public static List<BatchWithStatus> of(@NonNull List<BatchWithStatus> reviewList) {
        List<BatchWithStatus> result = new ArrayList<>();
        for (BatchWithStatus item : reviewList) {
            if (belongs(item)) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** True for a category whose items are renewed, not bought: documents and subscriptions. */
    public static boolean isRenewed(@NonNull ItemCategory category) {
        return RENEWED.contains(category);
    }

    /**
     * True for a document or subscription with something left whose status is "Date is today"
     * or "Date passed".
     */
    public static boolean belongs(@NonNull BatchWithStatus item) {
        BatchInfo batch = item.getBatch();
        DateStatus status = item.getStatus();
        return isRenewed(batch.getCategory()) && batch.getQuantity() >= 1
                && (status == DateStatus.TODAY || status == DateStatus.PAST_DATE);
    }
}

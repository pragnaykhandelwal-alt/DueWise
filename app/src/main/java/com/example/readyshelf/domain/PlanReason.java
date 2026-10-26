package com.example.readyshelf.domain;

/**
 * Why a swap plan treats a batch the way it does. The swap rules (the student's RotationPlanner)
 * choose a reason for each note; the app shows it as text.
 *
 * <p>Plans are never stored, so a constant can be removed or renamed without a database change.
 * On 2026-10-22 the reasons about the food class and the condition answers were removed, because
 * the swap rules no longer use them (the student's choice).
 */
public enum PlanReason {
    /**
     * Medicine, a document, or a subscription: never swapped (the student's choice on 2026-10-22;
     * until then everything but Food, as NOT_FOOD). Also a category added later that the swap
     * rules don't name yet.
     */
    CATEGORY_NOT_SWAPPED,
    /** No complete date is recorded. */
    DATE_MISSING_OR_INCOMPLETE,
    /** The date and its label type are not confirmed. */
    DATE_NOT_CONFIRMED,
    /** The label type is not one the swap rules accept. */
    LABEL_TYPE_NOT_KNOWN,
    /** The date is today or has passed. */
    DATE_TODAY_OR_PASSED,
    /** The date is not near yet. */
    NOT_NEAR_DATE,
    /** Swaps are proposed for the whole quantity. */
    SWAP_PROPOSED,
    /** Swaps are proposed for part of the quantity. */
    PARTLY_REPLACED,
    /** No pantry batch qualifies as a replacement. */
    NO_REPLACEMENT
}

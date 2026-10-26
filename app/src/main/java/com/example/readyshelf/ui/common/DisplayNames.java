package com.example.readyshelf.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.readyshelf.R;
import com.example.readyshelf.data.BatchEventType;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.PlanReason;

/**
 * The words the screens show for each enum constant. The words live in strings.xml.
 * (A switch on an enum is fine in Java. Only R.id values can't be switch cases with AGP 9.)
 */
public final class DisplayNames {

    private DisplayNames() {
        // Only static methods, so no objects of this class are made.
    }

    @StringRes
    public static int category(@NonNull ItemCategory category) {
        switch (category) {
            case FOOD:
                return R.string.category_food;
            case WATER:
                return R.string.category_water;
            case MEDICINE:
                return R.string.category_medicine;
            case FIRST_AID_SUPPLY:
                return R.string.category_first_aid_supply;
            case COSMETIC:
                return R.string.category_cosmetic;
            case DOCUMENT:
                return R.string.category_document;
            case SUBSCRIPTION:
                return R.string.category_subscription;
            case OTHER:
                return R.string.category_other;
            default:
                throw new IllegalArgumentException("No name for category " + category);
        }
    }

    @StringRes
    public static int locationType(@NonNull LocationType type) {
        switch (type) {
            case PANTRY:
                return R.string.location_type_pantry;
            case EMERGENCY_KIT:
                return R.string.location_type_emergency_kit;
            case FIRST_AID:
                return R.string.location_type_first_aid;
            case OTHER:
                return R.string.location_type_other;
            default:
                throw new IllegalArgumentException("No name for location type " + type);
        }
    }

    @StringRes
    public static int dateLabel(@NonNull DateLabel label) {
        switch (label) {
            case BEST_BY:
                return R.string.date_label_best_by;
            case USE_BY:
                return R.string.date_label_use_by;
            case EXPIRES:
                return R.string.date_label_expires;
            case OTHER:
                return R.string.date_label_other;
            default:
                throw new IllegalArgumentException("No name for date label " + label);
        }
    }

    @StringRes
    public static int status(@NonNull DateStatus status) {
        switch (status) {
            case NEEDS_REVIEW:
                return R.string.status_needs_review;
            case PAST_DATE:
                return R.string.status_past_date;
            case TODAY:
                return R.string.status_today;
            case NEAR_DATE:
                return R.string.status_near_date;
            case LATER:
                return R.string.status_later;
            default:
                throw new IllegalArgumentException("No name for status " + status);
        }
    }

    /** The sentence for a swap plan's reason, for example "No pantry batch qualifies…". */
    @StringRes
    public static int planReason(@NonNull PlanReason reason) {
        switch (reason) {
            case CATEGORY_NOT_SWAPPED:
                return R.string.plan_reason_category_not_swapped;
            case DATE_MISSING_OR_INCOMPLETE:
                return R.string.plan_reason_date_missing_or_incomplete;
            case DATE_NOT_CONFIRMED:
                return R.string.plan_reason_date_not_confirmed;
            case LABEL_TYPE_NOT_KNOWN:
                return R.string.plan_reason_label_type_not_known;
            case DATE_TODAY_OR_PASSED:
                return R.string.plan_reason_date_today_or_passed;
            case NOT_NEAR_DATE:
                return R.string.plan_reason_not_near_date;
            case SWAP_PROPOSED:
                return R.string.plan_reason_swap_proposed;
            case PARTLY_REPLACED:
                return R.string.plan_reason_partly_replaced;
            case NO_REPLACEMENT:
                return R.string.plan_reason_no_replacement;
            default:
                throw new IllegalArgumentException("No sentence for reason " + reason);
        }
    }

    /** The heading of a history entry, for example "Recorded as used". */
    @StringRes
    public static int eventType(@NonNull BatchEventType type) {
        switch (type) {
            case ADDED:
                return R.string.history_type_added;
            case EDITED:
                return R.string.history_type_edited;
            case MOVED:
                return R.string.history_type_moved;
            case USED:
                return R.string.history_type_used;
            case DISCARDED:
                return R.string.history_type_discarded;
            case ARCHIVED:
                return R.string.history_type_archived;
            case SWAPPED:
                return R.string.history_type_swapped;
            default:
                throw new IllegalArgumentException("No name for history type " + type);
        }
    }
}

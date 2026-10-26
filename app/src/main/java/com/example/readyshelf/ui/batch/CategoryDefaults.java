package com.example.readyshelf.ui.batch;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.ItemCategory;

/**
 * What the batch form starts with for each category, so the household has less to change.
 * Each value is only a starting point that the form shows: the household can change it, and
 * nothing is saved before the review. None of it is a rule about the item itself.
 */
public final class CategoryDefaults {

    private CategoryDefaults() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * The label type the form shows until the household (or a label photo) chooses one:
     * "Expires" for items whose labels usually say so, and "Best by" otherwise.
     */
    @NonNull
    public static DateLabel startingLabel(@NonNull ItemCategory category) {
        switch (category) {
            case MEDICINE:
            case FIRST_AID_SUPPLY:
            case COSMETIC:
            case DOCUMENT:
            case SUBSCRIPTION:
                return DateLabel.EXPIRES;
            default:
                return DateLabel.BEST_BY;
        }
    }

    /**
     * True for a document or a subscription: one item with no package, so a new batch of it is
     * saved as 1 and the form doesn't ask for a quantity or a unit.
     */
    public static boolean isOneItem(@NonNull ItemCategory category) {
        return category == ItemCategory.DOCUMENT || category == ItemCategory.SUBSCRIPTION;
    }

    /** The unit saved with a new product of a one-item category ("document"), or 0 for others. */
    @StringRes
    public static int oneItemUnit(@NonNull ItemCategory category) {
        switch (category) {
            case DOCUMENT:
                return R.string.unit_document;
            case SUBSCRIPTION:
                return R.string.unit_subscription;
            default:
                return 0;
        }
    }

    /** The units the form offers as one-tap choices for a new product of this category. */
    @ArrayRes
    public static int unitSuggestions(@NonNull ItemCategory category) {
        switch (category) {
            case FOOD:
                return R.array.units_food;
            case WATER:
                return R.array.units_water;
            case MEDICINE:
                return R.array.units_medicine;
            case FIRST_AID_SUPPLY:
                return R.array.units_first_aid_supply;
            case COSMETIC:
                return R.array.units_cosmetic;
            case DOCUMENT:
                return R.array.units_document;
            case SUBSCRIPTION:
                return R.array.units_subscription;
            default:
                return R.array.units_other;
        }
    }
}

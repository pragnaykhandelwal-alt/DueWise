package com.example.readyshelf.ui.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.ItemCategory;

/**
 * The icon of each category on the batch cards. TalkBack reads the category word with it
 * (DisplayNames.category), so the icon never shows something that is not also said.
 */
public final class CategoryLook {

    private CategoryLook() {
        // Only static methods, so no objects of this class are made.
    }

    @DrawableRes
    public static int icon(@NonNull ItemCategory category) {
        switch (category) {
            case FOOD:
                return R.drawable.ic_restaurant;
            case WATER:
                return R.drawable.ic_water_drop;
            case MEDICINE:
                return R.drawable.ic_medication;
            case FIRST_AID_SUPPLY:
                return R.drawable.ic_medical_services;
            case COSMETIC:
                return R.drawable.ic_spa;
            case DOCUMENT:
                return R.drawable.ic_description;
            case SUBSCRIPTION:
                return R.drawable.ic_autorenew;
            case OTHER:
                // Shapes, not Inventory's box (since 2026-10-22), so the box means only Inventory.
                return R.drawable.ic_category;
            default:
                throw new IllegalArgumentException("No icon for category " + category);
        }
    }
}

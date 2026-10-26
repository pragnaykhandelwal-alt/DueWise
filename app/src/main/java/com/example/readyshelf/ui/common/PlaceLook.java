package com.example.readyshelf.ui.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.LocationType;

/**
 * The icon of each place type in the swap picture and on Batch details. The place's name is always
 * written next to it. Since 2026-10-22 the pantry gets Material's shelves instead of the
 * reminder's shelf, and other places get a map pin instead of Inventory's box, so neither shares
 * a picture with something unrelated.
 */
public final class PlaceLook {

    private PlaceLook() {
        // Only static methods, so no objects of this class are made.
    }

    @DrawableRes
    public static int icon(@NonNull LocationType type) {
        switch (type) {
            case PANTRY:
                return R.drawable.ic_shelves;
            case EMERGENCY_KIT:
                return R.drawable.ic_backpack;
            case FIRST_AID:
                return R.drawable.ic_medical_services;
            case OTHER:
                return R.drawable.ic_location;
            default:
                throw new IllegalArgumentException("No icon for place type " + type);
        }
    }
}

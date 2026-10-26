package com.example.readyshelf.ui.common;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

/**
 * Whether the phone's text is large for its screen width. Things that normally sit side by side
 * (the places in the swap picture, Inventory's date-order buttons, Home's tiles, Batch details'
 * buttons) then go under each other, so no word is split or cut off.
 */
public final class LargeText {

    // Side by side needs at least this much screen width (in dp) for each unit of font scale:
    // a 411 dp wide phone keeps rows up to font scale 1.37.
    private static final int MIN_ROW_WIDTH_DP = 300;

    private LargeText() {
        // Only static methods, so no objects of this class are made.
    }

    /** True when side-by-side choices should go under each other. */
    public static boolean stackRows(@NonNull Resources resources) {
        Configuration config = resources.getConfiguration();
        return config.screenWidthDp < MIN_ROW_WIDTH_DP * config.fontScale;
    }

    /**
     * With large text, puts the views of a side-by-side row under each other, each the full
     * width, {@code gap} pixels apart. Otherwise the row stays as it is.
     */
    public static void stackIfLarge(@NonNull LinearLayout row, int gap) {
        if (!stackRows(row.getResources())) {
            return;
        }
        row.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.weight = 0;
            params.setMarginStart(0);
            params.topMargin = i == 0 ? 0 : gap;
            child.setLayoutParams(params);
        }
    }
}

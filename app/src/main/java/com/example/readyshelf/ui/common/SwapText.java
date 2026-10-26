package com.example.readyshelf.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.RuleRunner;

/**
 * The sentences that describe one proposed swap, shared by Home and the Review swap screen.
 * The kit batch is always "Move to pantry for review": ReadyShelf never says the item is safe
 * or that it must be used.
 */
public final class SwapText {

    private SwapText() {
        // Only static methods, so no objects of this class are made.
    }

    /** "Move to pantry for review: 3 cans (Best by: 20 Oct 2026) from Earthquake kit to Pantry." */
    @NonNull
    public static String moveOut(@NonNull Context context, @NonNull RuleRunner.ShownSwap swap) {
        BatchInfo kit = swap.getKitBatch();
        BatchInfo pantry = swap.getPantryBatch();
        return context.getString(R.string.swap_move_out,
                BatchText.amount(context, swap.getQuantity(), kit.getUnit()),
                BatchText.dateEntry(context, kit), kit.getLocationName(), pantry.getLocationName());
    }

    /** "Into Earthquake kit in its place: 3 cans (Best by: 1 Jun 2027) from Pantry." */
    @NonNull
    public static String moveIn(@NonNull Context context, @NonNull RuleRunner.ShownSwap swap) {
        BatchInfo kit = swap.getKitBatch();
        BatchInfo pantry = swap.getPantryBatch();
        return context.getString(R.string.swap_move_in, kit.getLocationName(),
                BatchText.amount(context, swap.getQuantity(), pantry.getUnit()),
                BatchText.dateEntry(context, pantry), pantry.getLocationName());
    }

    /** "Canned beans, 3 cans": what the "Review swap" button reads aloud after its label. */
    @NonNull
    public static String buttonDescription(@NonNull Context context,
                                           @NonNull RuleRunner.ShownSwap swap) {
        BatchInfo kit = swap.getKitBatch();
        return context.getString(R.string.swap_review_button_description, kit.getName(),
                BatchText.amount(context, swap.getQuantity(), kit.getUnit()));
    }
}

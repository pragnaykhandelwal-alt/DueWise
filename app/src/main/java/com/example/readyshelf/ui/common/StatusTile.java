package com.example.readyshelf.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateStatus;

/**
 * A status tile (item_status_tile.xml): on Home's grid, and since 2026-10-23 (the student's
 * choice) as the header of the list it opens in Inventory, so the list shows its status the way
 * the tile does.
 */
public final class StatusTile {

    private StatusTile() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Shows one status on a tile: the status icon in a circle of the status's label colors (the
     * circle in the label's color, the icon in the color of its words), the count in the status's
     * accent color (StatusLook), and the status word. The word is the same as on Inventory's Status
     * chips and on the status labels ("Near date"). TalkBack reads the whole tile as one line, for
     * example "Date passed: 3 batches".
     */
    public static void bind(@NonNull View tile, @NonNull DateStatus status, int count) {
        Context context = tile.getContext();
        ImageView icon = tile.findViewById(R.id.tile_icon);
        icon.setImageResource(StatusLook.icon(status));
        icon.setImageTintList(ColorStateList.valueOf(StatusLook.onContainerColor(context, status)));
        icon.setBackgroundTintList(
                ColorStateList.valueOf(StatusLook.containerColor(context, status)));
        TextView number = tile.findViewById(R.id.tile_count);
        number.setText(String.valueOf(count));
        number.setTextColor(StatusLook.accentColor(context, status));
        TextView word = tile.findViewById(R.id.tile_word);
        int wordId = DisplayNames.status(status);
        word.setText(wordId);
        tile.setContentDescription(context.getResources().getQuantityString(
                R.plurals.home_attention_line, count, context.getString(wordId), count));
    }

    /**
     * Makes a tile the header of a list: no arrow and no tap, because it isn't a way to another
     * screen, and TalkBack hears it as a heading, so it can jump to it.
     */
    public static void showAsHeader(@NonNull View tile) {
        tile.findViewById(R.id.tile_arrow).setVisibility(View.GONE);
        tile.setClickable(false);
        tile.setFocusable(false);
        ViewCompat.setAccessibilityHeading(tile, true);
    }
}

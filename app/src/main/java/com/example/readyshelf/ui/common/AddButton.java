package com.example.readyshelf.ui.common;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/**
 * The floating "Add batch" button on Inventory (Material's extended floating action button).
 * Home had one too until 2026-10-23, when Add batch became the last tile of Home's grid (the
 * student's choice). It shrinks to a round "+" while the household scrolls down, so it covers
 * less of the screen, and shows its words again when they scroll back up (Material's usual
 * behaviour). With large text it stays a round "+", because its words would cover a large part of
 * the screen (at font scale 2.0, on Home, it covered half of the Settings tile). Shrinking only
 * hides the words on screen, so TalkBack always hears "Add batch", and a long press shows them as
 * a tooltip.
 */
public final class AddButton {

    private AddButton() {
        // Only static methods, so no objects of this class are made.
    }

    /** Lets {@code button} shrink and grow with the scrolling of {@code scroll}. */
    public static void setUp(@NonNull ExtendedFloatingActionButton button,
                             @NonNull NestedScrollView scroll) {
        if (LargeText.stackRows(button.getResources())) {
            button.setExtended(false); // at once, without the shrinking animation
            return;
        }
        scroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollY > oldScrollY) {
                        button.shrink();
                    } else if (scrollY < oldScrollY || scrollY == 0) {
                        button.extend();
                    }
                });
    }
}

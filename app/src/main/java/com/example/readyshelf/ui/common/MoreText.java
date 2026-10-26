package com.example.readyshelf.ui.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;

import com.example.readyshelf.R;
import com.google.android.material.button.MaterialButton;

/**
 * A "More about this" button that shows or hides a longer explanation right below it, so a
 * screen starts short. Safety notes are never folded away; only how-it-works text is (and, on
 * Inventory, the filters under "Filters").
 * TalkBack hears whether the text is "Collapsed" or "Expanded". The screen keeps the choice
 * through a rotation with {@link #save}.
 */
public final class MoreText {

    private MoreText() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Connects a toggle (style ReadyShelf.MoreToggle) to the view it shows and hides. The
     * details view needs an id, so its state can be saved.
     */
    public static void setUp(@NonNull MaterialButton toggle, @NonNull View details,
                             @Nullable Bundle savedState) {
        setUp(toggle, details, savedState, R.string.more_show);
    }

    /** Like {@link #setUp(MaterialButton, View, Bundle)}, with other words than "More about this". */
    public static void setUp(@NonNull MaterialButton toggle, @NonNull View details,
                             @Nullable Bundle savedState, @StringRes int showText) {
        setUp(toggle, details, savedState, showText, R.string.more_hide, null);
    }

    /**
     * Like {@link #setUp(MaterialButton, View, Bundle)}, with other words while the details are
     * hidden ({@code showText}) and shown ({@code hideText}). {@code onChange}, if there is one,
     * runs after each tap, when the details have been shown or hidden.
     */
    public static void setUp(@NonNull MaterialButton toggle, @NonNull View details,
                             @Nullable Bundle savedState, @StringRes int showText,
                             @StringRes int hideText, @Nullable Runnable onChange) {
        boolean expanded = savedState != null && savedState.getBoolean(key(details), false);
        show(toggle, details, expanded, showText, hideText);
        toggle.setOnClickListener(view -> {
            show(toggle, details, details.getVisibility() != View.VISIBLE, showText, hideText);
            if (onChange != null) {
                onChange.run();
            }
        });
    }

    /** Remembers which explanations are shown, for {@link #setUp} after a rotation. */
    public static void save(@NonNull Bundle outState, @NonNull View... details) {
        for (View view : details) {
            outState.putBoolean(key(view), view.getVisibility() == View.VISIBLE);
        }
    }

    private static void show(@NonNull MaterialButton toggle, @NonNull View details,
                             boolean expanded, @StringRes int showText, @StringRes int hideText) {
        details.setVisibility(expanded ? View.VISIBLE : View.GONE);
        toggle.setText(expanded ? hideText : showText);
        toggle.setIconResource(expanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        ViewCompat.setStateDescription(toggle, toggle.getContext().getString(
                expanded ? R.string.more_state_expanded : R.string.more_state_collapsed));
    }

    @NonNull
    private static String key(@NonNull View details) {
        return "more_text_" + details.getId();
    }
}

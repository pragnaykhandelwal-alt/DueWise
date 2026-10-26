package com.example.readyshelf.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateStatus;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.Objects;

/**
 * How a date status looks: an icon, the status word, and its colors.
 *
 * <p>The word carries the meaning (SPEC "Accessibility"); the icon and the color only help.
 * Since 2026-10-23 (the student's design B1) "Date passed" is red, "Date is today" brass, "Near
 * date" cream, and "Date needs review" stays pale blue. "Later date" is plain gray with a
 * calendar, not a check mark, because DueWise never says that food is safe.
 *
 * <p>Each status has three colors (values/colors.xml): the label's color, which is also the
 * circle behind the icon on Home's tile; the color of the words and the icon on a label, which is
 * also the tile's icon; and an accent for the number on Home's tile.
 */
public final class StatusLook {

    // An icon is a little taller than the letters next to it, so it grows with the font size.
    private static final float ICON_SCALE = 1.25f;

    private StatusLook() {
        // Only static methods, so no objects of this class are made.
    }

    @DrawableRes
    public static int icon(@NonNull DateStatus status) {
        switch (status) {
            case PAST_DATE:
                return R.drawable.ic_error;
            case TODAY:
                return R.drawable.ic_today;
            case NEAR_DATE:
                return R.drawable.ic_clock;
            case NEEDS_REVIEW:
                return R.drawable.ic_help;
            case LATER:
                return R.drawable.ic_calendar;
            default:
                throw new IllegalArgumentException("No icon for status " + status);
        }
    }

    /** The color of a status label, and of the circle behind a tile's icon. */
    @ColorInt
    public static int containerColor(@NonNull Context context, @NonNull DateStatus status) {
        switch (status) {
            case PAST_DATE:
                return ContextCompat.getColor(context, R.color.status_passed_container);
            case TODAY:
                return ContextCompat.getColor(context, R.color.status_today_container);
            case NEAR_DATE:
                return ContextCompat.getColor(context, R.color.status_near_container);
            case NEEDS_REVIEW:
                return ContextCompat.getColor(context, R.color.status_review_container);
            case LATER:
                return themeColor(context,
                        com.google.android.material.R.attr.colorSurfaceContainerHighest);
            default:
                throw new IllegalArgumentException("No color for status " + status);
        }
    }

    /** The color of the word and the icon on a status label, and of the icon on a tile. */
    @ColorInt
    public static int onContainerColor(@NonNull Context context, @NonNull DateStatus status) {
        switch (status) {
            case PAST_DATE:
                return ContextCompat.getColor(context, R.color.status_on_passed_container);
            case TODAY:
                return ContextCompat.getColor(context, R.color.status_on_today_container);
            case NEAR_DATE:
                return ContextCompat.getColor(context, R.color.status_on_near_container);
            case NEEDS_REVIEW:
                return ContextCompat.getColor(context, R.color.status_on_review_container);
            case LATER:
                return themeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
            default:
                throw new IllegalArgumentException("No color for status " + status);
        }
    }

    /** The color of the number on a tile. */
    @ColorInt
    public static int accentColor(@NonNull Context context, @NonNull DateStatus status) {
        switch (status) {
            case PAST_DATE:
                return ContextCompat.getColor(context, R.color.status_passed_accent);
            case TODAY:
                return ContextCompat.getColor(context, R.color.status_today_accent);
            case NEAR_DATE:
                return ContextCompat.getColor(context, R.color.status_near_accent);
            case NEEDS_REVIEW:
                return ContextCompat.getColor(context, R.color.status_review_accent);
            case LATER:
                return themeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
            default:
                throw new IllegalArgumentException("No color for status " + status);
        }
    }

    /**
     * Shows a status on a label: the icon, the word ("Near date"), and the colors.
     * TalkBack reads the whole status line, "Status: Near date".
     */
    public static void showLabel(@NonNull TextView label, @NonNull DateStatus status) {
        Context context = label.getContext();
        label.setText(DisplayNames.status(status));
        label.setContentDescription(BatchText.statusLine(context, status));
        int words = onContainerColor(context, status);
        label.setTextColor(words);
        setStartIcon(label, icon(status), words);
        label.setBackground(roundedBackground(context, containerColor(context, status),
                R.dimen.label_corner_radius));
    }

    /** A rounded background in one color, with the given corner size (a dimension resource). */
    @NonNull
    public static Drawable roundedBackground(@NonNull Context context, @ColorInt int fill,
                                             @DimenRes int cornerRadius) {
        float radius = context.getResources().getDimension(cornerRadius);
        MaterialShapeDrawable background = new MaterialShapeDrawable(
                ShapeAppearanceModel.builder().setAllCornerSizes(radius).build());
        background.setFillColor(ColorStateList.valueOf(fill));
        return background;
    }

    /**
     * Puts an icon before a label's text, in the given color. It is sized from the text, so it
     * grows with the phone's font size like the words next to it.
     */
    public static void setStartIcon(@NonNull TextView label, @DrawableRes int iconId,
                                    @ColorInt int color) {
        Drawable icon = Objects.requireNonNull(
                ContextCompat.getDrawable(label.getContext(), iconId), "icon").mutate();
        icon.setTint(color);
        int size = Math.round(label.getTextSize() * ICON_SCALE);
        icon.setBounds(0, 0, size, size);
        label.setCompoundDrawablesRelative(icon, null, null, null);
    }

    @ColorInt
    private static int themeColor(@NonNull Context context, int attribute) {
        return MaterialColors.getColor(context, attribute, StatusLook.class.getSimpleName());
    }
}

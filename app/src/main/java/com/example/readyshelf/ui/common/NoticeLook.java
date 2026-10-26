package com.example.readyshelf.ui.common;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.example.readyshelf.R;
import com.google.android.material.color.MaterialColors;

/**
 * Gives a message box the look of a note, a warning, or an error while the screen is open, the
 * same looks as the styles ReadyShelf.Notice, ReadyShelf.Notice.Warning, and
 * ReadyShelf.Notice.Error. Used for boxes that show a note most of the time and an error when the
 * rules stop with one, and for Settings' notification box (a warning only while the daily
 * reminder is on but can't appear).
 */
public final class NoticeLook {

    private NoticeLook() {
        // Only static methods, so no objects of this class are made.
    }

    /** A quiet note: rounded, no sign. */
    public static void note(@NonNull TextView box) {
        box.setBackgroundResource(R.drawable.bg_notice);
        box.setTextColor(MaterialColors.getColor(box,
                com.google.android.material.R.attr.colorOnSecondaryContainer));
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(box, 0, 0, 0, 0);
    }

    /**
     * A warning: pale brass with dark words and a dark brass warning sign (gold until 2026-10-23).
     * The words still say what is wrong.
     */
    public static void warning(@NonNull TextView box) {
        box.setBackgroundResource(R.drawable.bg_notice_warning);
        box.setTextColor(MaterialColors.getColor(box,
                com.google.android.material.R.attr.colorOnSurface));
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(box, R.drawable.ic_warning,
                0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(box, ColorStateList.valueOf(
                ContextCompat.getColor(box.getContext(), R.color.brass_dark)));
        box.setCompoundDrawablePadding(box.getResources().getDimensionPixelSize(
                R.dimen.notice_icon_padding));
    }

    /** An error: red with an error sign. The words still say what is wrong. */
    public static void error(@NonNull TextView box) {
        int words = MaterialColors.getColor(box,
                com.google.android.material.R.attr.colorOnErrorContainer);
        box.setBackgroundResource(R.drawable.bg_notice_error);
        box.setTextColor(words);
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(box, R.drawable.ic_error,
                0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(box, ColorStateList.valueOf(words));
        box.setCompoundDrawablePadding(box.getResources().getDimensionPixelSize(
                R.dimen.notice_icon_padding));
    }
}

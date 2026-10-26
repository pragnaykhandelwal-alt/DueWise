package com.example.readyshelf.ui.photo;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.PhotoDateChoices;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.DisplayNames;

import java.time.LocalDate;

/** How a date chosen from a photo is written on the photo screen and on the batch form. */
public final class PhotoText {

    private PhotoText() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * "Best by: 26 Oct 2026", or "26 Oct 2026" when no label words were read next to the date.
     * An incomplete date is written as printed: "Expires: “03/2027” (incomplete date)".
     */
    @NonNull
    public static String date(@NonNull Context context, @NonNull PhotoDateChoices.Choice choice) {
        DateLabel label = choice.getLabel();
        LocalDate date = choice.getDate();
        if (date != null) {
            return label == null ? BatchText.date(date) : BatchText.labeledDate(context, date, label);
        }
        String printed = choice.getPrintedText() == null ? "" : choice.getPrintedText();
        return label == null ? context.getString(R.string.photo_incomplete_date, printed)
                : context.getString(R.string.labeled_incomplete_date,
                        context.getString(DisplayNames.dateLabel(label)), printed);
    }
}

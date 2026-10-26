package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * How the date on a package was recorded, and when the screens show "Date needs review".
 *
 * <p>This is not a date status: it never compares a date with today. Date statuses
 * (for example Near date or Date passed) come only from the student's DatePolicy.
 * This is plain Java, so it can be tested without a phone.
 */
public final class DateEntry {

    // The years the form's date picker shows, so a filled-in date can also be chosen there.
    private static final int FIRST_YEAR = 1900;
    private static final int LAST_YEAR = 2100;

    /** The three ways the batch form can record the date on a package. */
    public enum Kind {
        /** A full date (day, month, and year), chosen with the date picker. */
        COMPLETE,
        /** Only part of a date is printed or readable, for example "03/2027". Kept as text. */
        INCOMPLETE,
        /** No date is printed. */
        NONE
    }

    private DateEntry() {
        // Only static methods, so no objects of this class are made.
    }

    /** The kind of entry these saved values make. A blank printed text counts as no text. */
    @NonNull
    public static Kind kindOf(@Nullable LocalDate date, @Nullable String printedDateText) {
        if (date != null) {
            return Kind.COMPLETE;
        }
        if (printedDateText != null && !printedDateText.trim().isEmpty()) {
            return Kind.INCOMPLETE;
        }
        return Kind.NONE;
    }

    /** The kind of entry one batch has. */
    @NonNull
    public static Kind kindOf(@NonNull BatchInfo batch) {
        return kindOf(batch.getDate(), batch.getPrintedDateText());
    }

    /**
     * True when the screens show "Date needs review": there is no complete date, or the
     * household hasn't confirmed the complete date and its label type yet.
     */
    public static boolean needsReview(@Nullable LocalDate date, boolean confirmed) {
        return date == null || !confirmed;
    }

    /** True when the screens show "Date needs review" for this batch. */
    public static boolean needsReview(@NonNull BatchInfo batch) {
        return needsReview(batch.getDate(), batch.isDateConfirmed());
    }

    /**
     * The date that the form's "Use the last day of the month" button offers for an incomplete
     * date: the last day of the month when the printed text shows exactly one month and year,
     * for example 31 Mar 2027 for "03/2027". Null for anything else, for example a full date,
     * "10/26", or "Mar ??", and for years the date picker doesn't show (before 1900, after 2100).
     *
     * <p>The form never uses this date by itself. The household taps the button, and the date
     * stays unconfirmed until they tick the confirmation (SPEC.md, "Date entry").
     */
    @Nullable
    public static LocalDate lastDayOfPrintedMonth(@Nullable String printedDateText) {
        if (printedDateText == null) {
            return null;
        }
        YearMonth month = PrintedMonthReader.printedMonth(printedDateText);
        if (month == null || month.getYear() < FIRST_YEAR || month.getYear() > LAST_YEAR) {
            return null;
        }
        return month.atEndOfMonth();
    }
}

package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The date input model: what was recorded about the date on one package.
 * The student's DatePolicy and RotationPlanner read it. An object of this class never changes.
 *
 * <p>It only holds the recorded facts. Deciding whether a date can be used is the job of
 * the student's rules.
 */
public final class DateInput {

    @Nullable private final LocalDate date;
    @Nullable private final String printedText;
    @NonNull private final DateLabel label;
    private final boolean confirmed;

    public DateInput(@Nullable LocalDate date, @Nullable String printedText,
                     @NonNull DateLabel label, boolean confirmed) {
        this.date = date;
        this.printedText = printedText;
        this.label = Objects.requireNonNull(label, "label");
        this.confirmed = confirmed;
    }

    /** The date input of one batch. */
    @NonNull
    public static DateInput from(@NonNull BatchInfo batch) {
        return new DateInput(batch.getDate(), batch.getPrintedDateText(), batch.getDateLabel(),
                batch.isDateConfirmed());
    }

    /** The complete date (day, month, and year), or null when no complete date was recorded. */
    @Nullable
    public LocalDate getDate() {
        return date;
    }

    /**
     * What the label shows when only part of a date was recorded (for example "03/2027"),
     * exactly as typed. Null when nothing like that was recorded.
     */
    @Nullable
    public String getPrintedText() {
        return printedText;
    }

    /** The label type the household chose: Best by, Use by, Expires, or Other date. */
    @NonNull
    public DateLabel getLabel() {
        return label;
    }

    /** True when the household confirmed that the date and its label type match the package. */
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DateInput)) {
            return false;
        }
        DateInput that = (DateInput) other;
        return confirmed == that.confirmed
                && Objects.equals(date, that.date)
                && Objects.equals(printedText, that.printedText)
                && label == that.label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, printedText, label, confirmed);
    }

    @NonNull
    @Override
    public String toString() {
        return "DateInput{date=" + date + ", printedText=" + printedText + ", label=" + label
                + ", confirmed=" + confirmed + "}";
    }
}

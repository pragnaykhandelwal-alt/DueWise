package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * One possible printed date that the student's date-candidate parser found in the text of a
 * label photo. It only holds what was read; it has no rules. An object of this class never
 * changes.
 *
 * <ul>
 *   <li>{@link Kind#COMPLETE}: a day, month, and year that can be read only one way
 *       ({@link #getDate()}).</li>
 *   <li>{@link Kind#TWO_READINGS}: numbers whose day and month could be swapped, such as
 *       "03/04/27". Both readings are kept ({@link #getMonthFirst()}, {@link #getDayFirst()}),
 *       and the household chooses.</li>
 *   <li>{@link Kind#MONTH_AND_YEAR}: only a month and a year are printed ({@link #getMonth()}).
 *       There is no day.</li>
 * </ul>
 *
 * <p>{@link #getRawText()} is the recognized text the date came from, unchanged (for example the
 * whole line). {@link #getDateText()} is the part of it that was read as the date.
 * {@link #getLabel()} is the label type from words such as "BEST BY", or null when there are
 * none. Getters for the other kinds return null.
 *
 * <p>Make candidates with {@link #complete}, {@link #twoReadings}, or {@link #monthAndYear}.
 * equals and toString cover every field, so tests can compare candidates with assertEquals.
 */
public final class DateCandidate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** What kind of date was read. */
    public enum Kind {
        /** A day, month, and year that can be read only one way. */
        COMPLETE,
        /** A day and month that could be swapped: both readings are kept. */
        TWO_READINGS,
        /** Only a month and a year: no day. */
        MONTH_AND_YEAR
    }

    @NonNull private final Kind kind;
    @NonNull private final String rawText;
    @NonNull private final String dateText;
    @Nullable private final DateLabel label;
    @Nullable private final LocalDate date;
    @Nullable private final LocalDate monthFirst;
    @Nullable private final LocalDate dayFirst;
    @Nullable private final YearMonth month;

    private DateCandidate(@NonNull Kind kind, @NonNull String rawText, @NonNull String dateText,
                          @Nullable DateLabel label, @Nullable LocalDate date,
                          @Nullable LocalDate monthFirst, @Nullable LocalDate dayFirst,
                          @Nullable YearMonth month) {
        this.kind = kind;
        this.rawText = Objects.requireNonNull(rawText, "rawText");
        this.dateText = Objects.requireNonNull(dateText, "dateText");
        this.label = label;
        this.date = date;
        this.monthFirst = monthFirst;
        this.dayFirst = dayFirst;
        this.month = month;
    }

    /**
     * A day, month, and year that can be read only one way.
     *
     * @param rawText  the recognized text the date came from, unchanged
     * @param dateText the part of {@code rawText} that was read as the date
     * @param label    the label type from the words next to the date, or null when there are none
     * @param date     the date
     */
    @NonNull
    public static DateCandidate complete(@NonNull String rawText, @NonNull String dateText,
                                         @Nullable DateLabel label, @NonNull LocalDate date) {
        return new DateCandidate(Kind.COMPLETE, rawText, dateText, label,
                Objects.requireNonNull(date, "date"), null, null, null);
    }

    /**
     * A day and month that could be swapped, such as "03/04/27". Both readings are kept, so
     * the household can choose.
     *
     * @param monthFirst the date when the first number is the month
     * @param dayFirst   the date when the first number is the day
     */
    @NonNull
    public static DateCandidate twoReadings(@NonNull String rawText, @NonNull String dateText,
                                            @Nullable DateLabel label,
                                            @NonNull LocalDate monthFirst,
                                            @NonNull LocalDate dayFirst) {
        return new DateCandidate(Kind.TWO_READINGS, rawText, dateText, label, null,
                Objects.requireNonNull(monthFirst, "monthFirst"),
                Objects.requireNonNull(dayFirst, "dayFirst"), null);
    }

    /** Only a month and a year are printed, such as "03/2027". There is no day. */
    @NonNull
    public static DateCandidate monthAndYear(@NonNull String rawText, @NonNull String dateText,
                                             @Nullable DateLabel label, @NonNull YearMonth month) {
        return new DateCandidate(Kind.MONTH_AND_YEAR, rawText, dateText, label, null, null, null,
                Objects.requireNonNull(month, "month"));
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    /** The recognized text the date came from, unchanged (for example the whole line). */
    @NonNull
    public String getRawText() {
        return rawText;
    }

    /** The part of the raw text that was read as the date, for example "03/04/27". */
    @NonNull
    public String getDateText() {
        return dateText;
    }

    /** The label type from words such as "BEST BY", or null when there are none. */
    @Nullable
    public DateLabel getLabel() {
        return label;
    }

    /** The date of a COMPLETE candidate; null for the other kinds. */
    @Nullable
    public LocalDate getDate() {
        return date;
    }

    /** The month-first reading of a TWO_READINGS candidate; null for the other kinds. */
    @Nullable
    public LocalDate getMonthFirst() {
        return monthFirst;
    }

    /** The day-first reading of a TWO_READINGS candidate; null for the other kinds. */
    @Nullable
    public LocalDate getDayFirst() {
        return dayFirst;
    }

    /** The month of a MONTH_AND_YEAR candidate; null for the other kinds. */
    @Nullable
    public YearMonth getMonth() {
        return month;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DateCandidate)) {
            return false;
        }
        DateCandidate that = (DateCandidate) other;
        return kind == that.kind
                && rawText.equals(that.rawText)
                && dateText.equals(that.dateText)
                && label == that.label
                && Objects.equals(date, that.date)
                && Objects.equals(monthFirst, that.monthFirst)
                && Objects.equals(dayFirst, that.dayFirst)
                && Objects.equals(month, that.month);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, rawText, dateText, label, date, monthFirst, dayFirst, month);
    }

    @NonNull
    @Override
    public String toString() {
        return "DateCandidate{" + kind + ", rawText=\"" + rawText + "\", dateText=\"" + dateText
                + "\", label=" + label + ", date=" + date + ", monthFirst=" + monthFirst
                + ", dayFirst=" + dayFirst + ", month=" + month + "}";
    }
}

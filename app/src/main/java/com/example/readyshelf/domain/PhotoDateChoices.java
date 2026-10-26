package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The possible dates that photo entry offers for one date photo, made from the answer of the
 * student's date-candidate parser (SPEC.md, "Photo entry").
 *
 * <p>This is the app's safety layer around the parser. It decides nothing about dates: it never
 * picks a date, and an error in the parser never closes the app. Every candidate becomes one or
 * more choices, in the parser's order:
 * <ul>
 *   <li>COMPLETE: one choice with that date.</li>
 *   <li>TWO_READINGS: three choices, because ReadyShelf can't tell which one the package means:
 *       the month-first date, the day-first date, and "keep as printed" (an incomplete date with
 *       the printed text). When both readings are the same date, there is one choice.</li>
 *   <li>MONTH_AND_YEAR: one choice that keeps the printed text as an incomplete date. No day is
 *       added.</li>
 * </ul>
 * A choice that is exactly the same as an earlier one is shown once. No choice is chosen
 * beforehand: the household taps one, or none.
 *
 * <p>This is plain Java, so it can be tested without a phone. It is Serializable, so the photo
 * screen can keep it when Android closes the app. An object of this class never changes.
 */
public final class PhotoDateChoices implements Serializable {

    private static final long serialVersionUID = 1L;

    /** What the parser's answer means for the review. */
    public enum Outcome {
        /** The student's parser is not connected yet (AppContainer holds null). */
        NOT_CONNECTED,
        /** No text was read on the photo, so the parser was not asked. */
        NO_TEXT,
        /** The parser found no date. */
        NO_DATE_FOUND,
        /** The parser stopped with an error, or broke its contract (see {@link #getError()}). */
        PARSER_FAILED,
        /** At least one choice. */
        CHOICES
    }

    /** One date the household can tap. An object of this class never changes. */
    public static final class Choice implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Which reading of the candidate this choice is. */
        public enum Reading {
            /** A complete date that can be read only one way. */
            ONLY_READING,
            /** The month-first reading of two possible readings. */
            MONTH_FIRST,
            /** The day-first reading of two possible readings. */
            DAY_FIRST,
            /** Two readings are possible, so the printed text is kept as an incomplete date. */
            AS_PRINTED,
            /** Only a month and a year are printed; kept as an incomplete date. */
            MONTH_AND_YEAR
        }

        @NonNull private final Reading reading;
        @Nullable private final LocalDate date;
        @Nullable private final String printedText;
        @Nullable private final DateLabel label;
        @NonNull private final String rawText;
        @NonNull private final String dateText;

        private Choice(@NonNull Reading reading, @Nullable LocalDate date,
                       @Nullable String printedText, @NonNull DateCandidate candidate) {
            this.reading = reading;
            this.date = date;
            this.printedText = printedText;
            this.label = candidate.getLabel();
            this.rawText = candidate.getRawText();
            this.dateText = candidate.getDateText();
        }

        @NonNull
        public Reading getReading() {
            return reading;
        }

        /** The complete date this choice fills in, or null when it is an incomplete date. */
        @Nullable
        public LocalDate getDate() {
            return date;
        }

        /** The printed text this choice keeps as an incomplete date, or null for a complete date. */
        @Nullable
        public String getPrintedText() {
            return printedText;
        }

        /** The label type from the words next to the date, or null when there were none. */
        @Nullable
        public DateLabel getLabel() {
            return label;
        }

        /** The recognized text the date came from, unchanged. It goes into the batch history. */
        @NonNull
        public String getRawText() {
            return rawText;
        }

        /** The part of the raw text that was read as the date. */
        @NonNull
        public String getDateText() {
            return dateText;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Choice)) {
                return false;
            }
            Choice that = (Choice) other;
            return reading == that.reading && Objects.equals(date, that.date)
                    && Objects.equals(printedText, that.printedText) && label == that.label
                    && rawText.equals(that.rawText) && dateText.equals(that.dateText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reading, date, printedText, label, rawText, dateText);
        }

        @NonNull
        @Override
        public String toString() {
            return "Choice{" + reading + ", date=" + date + ", printedText=" + printedText
                    + ", label=" + label + ", rawText=\"" + rawText + "\"}";
        }
    }

    @NonNull private final Outcome outcome;
    @NonNull private final List<Choice> choices;
    @Nullable private final String error;

    private PhotoDateChoices(@NonNull Outcome outcome, @NonNull List<Choice> choices,
                             @Nullable String error) {
        this.outcome = outcome;
        this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
        this.error = error;
    }

    /**
     * Asks the parser for the possible dates in the text of one date photo.
     *
     * @param parser the student's parser, or null while it is not connected
     * @param lines  the photo's text lines, as the text reader returned them
     * @param today  the phone's date, passed on to the parser
     */
    @NonNull
    public static PhotoDateChoices find(@Nullable DateCandidateParserContract parser,
                                        @NonNull List<String> lines, @NonNull LocalDate today) {
        if (parser == null) {
            return new PhotoDateChoices(Outcome.NOT_CONNECTED, Collections.emptyList(), null);
        }
        if (lines.isEmpty()) {
            return new PhotoDateChoices(Outcome.NO_TEXT, Collections.emptyList(), null);
        }
        // The parser gets its own copy, which can't be changed.
        List<String> input = Collections.unmodifiableList(new ArrayList<>(lines));
        List<Choice> choices = new ArrayList<>();
        try {
            List<DateCandidate> candidates;
            // Only one caller at a time asks the parser, as with the other rules (RuleRunner).
            synchronized (parser) {
                candidates = parser.findCandidates(input, today);
            }
            if (candidates == null) {
                return failed("findCandidates returned null");
            }
            for (DateCandidate candidate : candidates) {
                if (candidate == null) {
                    return failed("findCandidates returned a list with null in it");
                }
                addChoices(candidate, choices);
            }
        } catch (RuntimeException | StackOverflowError error) {
            // A mistake in the parser (for example a regular expression that runs too deep on
            // a long line) is shown as text on the screen instead of closing the app.
            return failed(error.toString());
        }
        if (choices.isEmpty()) {
            return new PhotoDateChoices(Outcome.NO_DATE_FOUND, choices, null);
        }
        return new PhotoDateChoices(Outcome.CHOICES, choices, null);
    }

    private static void addChoices(@NonNull DateCandidate candidate, @NonNull List<Choice> choices) {
        switch (candidate.getKind()) {
            case COMPLETE:
                add(choices, new Choice(Choice.Reading.ONLY_READING, candidate.getDate(), null, candidate));
                break;
            case TWO_READINGS:
                LocalDate monthFirst = candidate.getMonthFirst();
                LocalDate dayFirst = candidate.getDayFirst();
                if (Objects.equals(monthFirst, dayFirst)) {
                    // Both readings give the same day, so there is nothing to choose between.
                    add(choices, new Choice(Choice.Reading.ONLY_READING, monthFirst, null, candidate));
                } else {
                    add(choices, new Choice(Choice.Reading.MONTH_FIRST, monthFirst, null, candidate));
                    add(choices, new Choice(Choice.Reading.DAY_FIRST, dayFirst, null, candidate));
                    add(choices, new Choice(Choice.Reading.AS_PRINTED, null, candidate.getDateText(),
                            candidate));
                }
                break;
            default:
                add(choices, new Choice(Choice.Reading.MONTH_AND_YEAR, null, candidate.getDateText(),
                        candidate));
                break;
        }
    }

    private static void add(@NonNull List<Choice> choices, @NonNull Choice choice) {
        if (!choices.contains(choice)) {
            choices.add(choice);
        }
    }

    @NonNull
    private static PhotoDateChoices failed(@NonNull String error) {
        return new PhotoDateChoices(Outcome.PARSER_FAILED, Collections.emptyList(), error);
    }

    @NonNull
    public Outcome getOutcome() {
        return outcome;
    }

    /** The choices, in the parser's order. Empty unless the outcome is CHOICES. */
    @NonNull
    public List<Choice> getChoices() {
        return choices;
    }

    /** What went wrong, when the outcome is PARSER_FAILED; null otherwise. */
    @Nullable
    public String getError() {
        return error;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PhotoDateChoices)) {
            return false;
        }
        PhotoDateChoices that = (PhotoDateChoices) other;
        return outcome == that.outcome && choices.equals(that.choices)
                && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outcome, choices, error);
    }
}

package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.List;

/**
 * The date-candidate rules that photo entry needs: they read the text of a date photo and return
 * the possible printed dates.
 *
 * <p>DateCandidateParser.java (in this package) implements this interface, and
 * DateCandidateParserTest.java holds its main tests; AI-USE.md records who wrote them. The
 * requirements and decisions are listed in SPEC.md ("The student's rules"). Until the class is
 * connected in AppContainer, photo entry shows the text it read but no possible dates (SPEC.md,
 * "Turned off for now").
 *
 * <p>The parser only reads text. It never sees a photo, and it never decides which date is
 * right: the app shows every candidate, the household chooses one or none, and nothing is saved
 * until the household confirms the form.
 */
public interface DateCandidateParserContract {

    /**
     * Returns the possible printed dates in {@code lines}, in the order they appear. Must not
     * return null: return an empty list when no date is found. The list must not contain null.
     *
     * <p>What the app needs from the answer (the guide's requirements):
     * <ul>
     *   <li>When several dates are printed, return all of them. The household chooses.</li>
     *   <li>When the day and the month could be swapped (for example "03/04"), return one
     *       TWO_READINGS candidate with both readings. Never pick one.</li>
     *   <li>Text with only a month and a year is one MONTH_AND_YEAR candidate. Never add a
     *       day.</li>
     *   <li>A lot code is not a date.</li>
     *   <li>Every candidate keeps the raw text it came from, and the label type of the words
     *       next to it (or null when there are none).</li>
     * </ul>
     *
     * <p>Must not change {@code lines}: the app passes a list that can't be changed. The app
     * calls this on a background thread.
     *
     * @param lines the recognized text of the date photo, top to bottom, exactly as the text
     *              reader returned it. Lines can contain reading mistakes, such as the letter O
     *              for the digit 0.
     * @param today the phone's date. Tests can pass any fixed date. The answer must depend only
     *              on {@code lines} and {@code today}, never on the clock.
     */
    @NonNull
    List<DateCandidate> findCandidates(@NonNull List<String> lines, @NonNull LocalDate today);
}

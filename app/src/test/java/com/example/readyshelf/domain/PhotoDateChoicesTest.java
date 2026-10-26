package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.readyshelf.domain.PhotoDateChoices.Choice;
import com.example.readyshelf.domain.PhotoDateChoices.Outcome;

import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the app's safety layer around the student's date-candidate parser.
 *
 * <p>The parsers here are fakes: each one returns a preset answer and never looks at the text,
 * like FixedStatusPolicy in the reminder tests. They are not a date-candidate parser, and these
 * are not the student's parser tests.
 */
public class PhotoDateChoicesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final List<String> SOME_LINES = Arrays.asList("first line", "second line");
    private static final LocalDate MARCH_4 = LocalDate.of(2027, 3, 4);
    private static final LocalDate APRIL_3 = LocalDate.of(2027, 4, 3);

    /** A fake parser that always returns the same preset answer. */
    private static DateCandidateParserContract answering(DateCandidate... candidates) {
        List<DateCandidate> answer = Arrays.asList(candidates);
        return (lines, today) -> answer;
    }

    // ----- What happens before and around the parser -----

    @Test
    public void noParser_isNotConnected() {
        PhotoDateChoices result = PhotoDateChoices.find(null, SOME_LINES, TODAY);

        assertEquals(Outcome.NOT_CONNECTED, result.getOutcome());
        assertTrue(result.getChoices().isEmpty());
        assertNull(result.getError());
    }

    @Test
    public void noText_doesNotAskTheParser() {
        DateCandidateParserContract mustNotBeAsked = (lines, today) -> {
            fail("The parser was asked about a photo with no text");
            return Collections.emptyList();
        };

        PhotoDateChoices result = PhotoDateChoices.find(mustNotBeAsked,
                Collections.<String>emptyList(), TODAY);

        assertEquals(Outcome.NO_TEXT, result.getOutcome());
    }

    @Test
    public void theParserGetsTheLinesAndToday_unchanged() {
        List<List<String>> seenLines = new ArrayList<>();
        List<LocalDate> seenToday = new ArrayList<>();
        DateCandidateParserContract recording = (lines, today) -> {
            seenLines.add(lines);
            seenToday.add(today);
            return Collections.emptyList();
        };

        PhotoDateChoices.find(recording, SOME_LINES, TODAY);

        assertEquals(Collections.singletonList(SOME_LINES), seenLines);
        assertEquals(Collections.singletonList(TODAY), seenToday);
    }

    @Test
    public void aParserThatChangesTheLines_failsInsteadOfChangingThem() {
        List<String> lines = new ArrayList<>(SOME_LINES);
        DateCandidateParserContract changing = (given, today) -> {
            given.add("made up");
            return Collections.emptyList();
        };

        PhotoDateChoices result = PhotoDateChoices.find(changing, lines, TODAY);

        assertEquals(Outcome.PARSER_FAILED, result.getOutcome());
        assertEquals(SOME_LINES, lines);
    }

    @Test
    public void noCandidates_isNoDateFound() {
        PhotoDateChoices result = PhotoDateChoices.find(answering(), SOME_LINES, TODAY);

        assertEquals(Outcome.NO_DATE_FOUND, result.getOutcome());
        assertTrue(result.getChoices().isEmpty());
    }

    // ----- A parser that breaks never closes the app -----

    @Test
    public void aParserError_isShownAsText() {
        DateCandidateParserContract throwing = (lines, today) -> {
            throw new IllegalStateException("boom");
        };

        PhotoDateChoices result = PhotoDateChoices.find(throwing, SOME_LINES, TODAY);

        assertEquals(Outcome.PARSER_FAILED, result.getOutcome());
        assertEquals("java.lang.IllegalStateException: boom", result.getError());
        assertTrue(result.getChoices().isEmpty());
    }

    @Test
    public void aStackOverflow_isShownAsText() {
        DateCandidateParserContract tooDeep = (lines, today) -> {
            throw new StackOverflowError();
        };

        assertEquals(Outcome.PARSER_FAILED,
                PhotoDateChoices.find(tooDeep, SOME_LINES, TODAY).getOutcome());
    }

    @Test
    public void aNullAnswer_fails() {
        DateCandidateParserContract returnsNull = (lines, today) -> null;

        PhotoDateChoices result = PhotoDateChoices.find(returnsNull, SOME_LINES, TODAY);

        assertEquals(Outcome.PARSER_FAILED, result.getOutcome());
        assertEquals("findCandidates returned null", result.getError());
    }

    @Test
    public void aNullCandidate_fails() {
        PhotoDateChoices result = PhotoDateChoices.find(answering((DateCandidate) null),
                SOME_LINES, TODAY);

        assertEquals(Outcome.PARSER_FAILED, result.getOutcome());
    }

    // ----- How candidates become choices. Nothing is chosen beforehand. -----

    @Test
    public void aCompleteDate_isOneChoice() {
        DateCandidate complete = DateCandidate.complete("BEST BY 10/26/2027", "10/26/2027",
                DateLabel.BEST_BY, LocalDate.of(2027, 10, 26));

        PhotoDateChoices result = PhotoDateChoices.find(answering(complete), SOME_LINES, TODAY);

        assertEquals(Outcome.CHOICES, result.getOutcome());
        assertEquals(1, result.getChoices().size());
        Choice choice = result.getChoices().get(0);
        assertEquals(Choice.Reading.ONLY_READING, choice.getReading());
        assertEquals(LocalDate.of(2027, 10, 26), choice.getDate());
        assertNull(choice.getPrintedText());
        assertEquals(DateLabel.BEST_BY, choice.getLabel());
        assertEquals("BEST BY 10/26/2027", choice.getRawText());
        assertEquals("10/26/2027", choice.getDateText());
    }

    @Test
    public void twoReadings_areThreeChoices_soTheAppNeverPicksOne() {
        DateCandidate twoReadings = DateCandidate.twoReadings("EXP 03/04/27", "03/04/27",
                DateLabel.EXPIRES, MARCH_4, APRIL_3);

        List<Choice> choices = PhotoDateChoices.find(answering(twoReadings), SOME_LINES, TODAY)
                .getChoices();

        assertEquals(3, choices.size());
        assertEquals(Choice.Reading.MONTH_FIRST, choices.get(0).getReading());
        assertEquals(MARCH_4, choices.get(0).getDate());
        assertEquals(Choice.Reading.DAY_FIRST, choices.get(1).getReading());
        assertEquals(APRIL_3, choices.get(1).getDate());
        assertEquals(Choice.Reading.AS_PRINTED, choices.get(2).getReading());
        assertNull(choices.get(2).getDate());
        assertEquals("03/04/27", choices.get(2).getPrintedText());
        for (Choice choice : choices) {
            assertEquals(DateLabel.EXPIRES, choice.getLabel());
            assertEquals("EXP 03/04/27", choice.getRawText());
        }
    }

    @Test
    public void twoReadingsOfTheSameDay_areOneChoice() {
        LocalDate sameDay = LocalDate.of(2027, 5, 5);
        DateCandidate twoReadings = DateCandidate.twoReadings("USE BY 05/05/27", "05/05/27",
                DateLabel.USE_BY, sameDay, sameDay);

        List<Choice> choices = PhotoDateChoices.find(answering(twoReadings), SOME_LINES, TODAY)
                .getChoices();

        assertEquals(1, choices.size());
        assertEquals(Choice.Reading.ONLY_READING, choices.get(0).getReading());
        assertEquals(sameDay, choices.get(0).getDate());
    }

    @Test
    public void aMonthAndYear_staysAPrintedText_withNoDay() {
        DateCandidate monthOnly = DateCandidate.monthAndYear("EXP 03/2027", "03/2027",
                DateLabel.EXPIRES, YearMonth.of(2027, 3));

        List<Choice> choices = PhotoDateChoices.find(answering(monthOnly), SOME_LINES, TODAY)
                .getChoices();

        assertEquals(1, choices.size());
        assertEquals(Choice.Reading.MONTH_AND_YEAR, choices.get(0).getReading());
        assertNull(choices.get(0).getDate());
        assertEquals("03/2027", choices.get(0).getPrintedText());
    }

    @Test
    public void severalCandidates_keepTheParsersOrder_andRepeatsAppearOnce() {
        DateCandidate first = DateCandidate.complete("USE BY 11/20/2026", "11/20/2026",
                DateLabel.USE_BY, LocalDate.of(2026, 11, 20));
        DateCandidate second = DateCandidate.complete("BEST BY 10/25/2026", "10/25/2026",
                DateLabel.BEST_BY, LocalDate.of(2026, 10, 25));

        List<Choice> choices = PhotoDateChoices.find(answering(first, second, first),
                SOME_LINES, TODAY).getChoices();

        assertEquals(2, choices.size());
        assertEquals(LocalDate.of(2026, 11, 20), choices.get(0).getDate());
        assertEquals(LocalDate.of(2026, 10, 25), choices.get(1).getDate());
    }

    @Test
    public void noLabelWords_staysNoLabel() {
        DateCandidate unlabeled = DateCandidate.complete("10/26/2027", "10/26/2027", null,
                LocalDate.of(2027, 10, 26));

        assertNull(PhotoDateChoices.find(answering(unlabeled), SOME_LINES, TODAY)
                .getChoices().get(0).getLabel());
    }

    @Test
    public void choicesCantBeChanged() {
        PhotoDateChoices result = PhotoDateChoices.find(answering(DateCandidate.complete(
                "10/26/2027", "10/26/2027", null, LocalDate.of(2027, 10, 26))), SOME_LINES, TODAY);

        try {
            result.getChoices().clear();
            fail("The choices could be changed");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, result.getChoices().size());
        }
    }

    @Test
    public void survivesBeingSaved() throws IOException, ClassNotFoundException {
        PhotoDateChoices result = PhotoDateChoices.find(answering(DateCandidate.twoReadings(
                "EXP 03/04/27", "03/04/27", DateLabel.EXPIRES, MARCH_4, APRIL_3)), SOME_LINES, TODAY);

        Object copy = DateCandidateTest.roundTrip(result);

        assertEquals(result, copy);
        assertSame(PhotoDateChoices.class, copy.getClass());
    }
}

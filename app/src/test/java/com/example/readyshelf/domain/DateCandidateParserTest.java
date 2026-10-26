package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The main rule tests for DateCandidateParser: the possible printed dates it finds in the text of a
 * date photo (SPEC.md, "DateCandidateParser requirements" and its decisions D1 to D11).
 *
 * <p>Claude Code wrote these tests on 2026-10-18 at the student's request (CLAUDE.md rules 1 and
 * 2). The student approved every case before the parser was written (AI-USE.md). The label text
 * is typed here and made up; none of it was read from the household's packages.
 *
 * <p>Each expected answer is made with DateCandidate's factory methods. DateCandidate.equals
 * compares every value (the kind, the dates, the date text, the raw text, and the label), so one
 * assertEquals checks the whole answer.
 */
public class DateCandidateParserTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    private final DateCandidateParserContract parser = new DateCandidateParser();

    // ----- A. What the contract decides -----

    @Test
    public void labeledDate_isOneCompleteDate() {
        assertOnly(DateCandidate.complete("BEST BY 10/25/2026", "10/25/2026", DateLabel.BEST_BY,
                LocalDate.of(2026, 10, 25)), "BEST BY 10/25/2026");
    }

    @Test
    public void dateWithoutLabelWords_hasNoLabel() {
        assertOnly(DateCandidate.complete("10/25/2026", "10/25/2026", null,
                LocalDate.of(2026, 10, 25)), "10/25/2026");
    }

    @Test
    public void severalDates_comeBackInPrintedOrder() {
        // Printed order, not date order: the household chooses.
        List<DateCandidate> expected = Arrays.asList(
                DateCandidate.complete("USE BY 11/20/2026", "11/20/2026", DateLabel.USE_BY,
                        LocalDate.of(2026, 11, 20)),
                DateCandidate.complete("BEST BY 10/25/2026", "10/25/2026", DateLabel.BEST_BY,
                        LocalDate.of(2026, 10, 25)));

        assertEquals(expected, find("USE BY 11/20/2026", "BEST BY 10/25/2026"));
    }

    @Test
    public void twoDatesOnOneLine_labelWordsBelongToTheFirst() {
        String line = "BEST BY 10/25/2026 11/20/2026";
        List<DateCandidate> expected = Arrays.asList(
                DateCandidate.complete(line, "10/25/2026", DateLabel.BEST_BY,
                        LocalDate.of(2026, 10, 25)),
                DateCandidate.complete(line, "11/20/2026", null, LocalDate.of(2026, 11, 20)));

        assertEquals(expected, find(line));
    }

    @Test
    public void swappableDayAndMonth_isTwoReadings() {
        // March 4 or April 3: ReadyShelf can't tell, so both readings are kept, month first first.
        assertOnly(DateCandidate.twoReadings("EXP 03/04/2027", "03/04/2027", DateLabel.EXPIRES,
                LocalDate.of(2027, 3, 4), LocalDate.of(2027, 4, 3)), "EXP 03/04/2027");
    }

    @Test
    public void monthAndYear_hasNoDay() {
        YearMonth march2027 = YearMonth.of(2027, 3);

        assertOnly(DateCandidate.monthAndYear("EXP 03/2027", "03/2027", DateLabel.EXPIRES,
                march2027), "EXP 03/2027");
        assertOnly(DateCandidate.monthAndYear("EXP 2027-03", "2027-03", DateLabel.EXPIRES,
                march2027), "EXP 2027-03");
        assertOnly(DateCandidate.monthAndYear("EXP MAR 2027", "MAR 2027", DateLabel.EXPIRES,
                march2027), "EXP MAR 2027");
        assertOnly(DateCandidate.monthAndYear("EXP 2027 MAR", "2027 MAR", DateLabel.EXPIRES,
                march2027), "EXP 2027 MAR");
    }

    @Test
    public void lotCode_isNotADate() {
        assertNoDates("LOT 2026-10-28");
    }

    @Test
    public void textWithoutADate_givesAnEmptyList() {
        assertNoDates("NUTRITION FACTS", "2000 CALORIES A DAY", "1/2 CUP (40g)", "$10.26",
                "BAKED AT 12:30", "CALL 1-800-555-1234", "NDC 0573-0164-40");
    }

    @Test
    public void noLines_givesAnEmptyList() {
        assertEquals(Collections.emptyList(),
                parser.findCandidates(Collections.<String>emptyList(), TODAY));
        assertNoDates("");
    }

    @Test
    public void linesThatCantBeChanged_areOnlyRead() {
        // The app passes a list that can't be changed (PhotoDateChoices). Changing it would throw.
        List<String> lines = Collections.unmodifiableList(Arrays.asList("BEST BY 10/25/2026"));

        assertEquals(Collections.singletonList(DateCandidate.complete("BEST BY 10/25/2026",
                "10/25/2026", DateLabel.BEST_BY, LocalDate.of(2026, 10, 25))),
                parser.findCandidates(lines, TODAY));
        assertEquals(Collections.singletonList("BEST BY 10/25/2026"), lines);
    }

    // ----- B. The decisions D1 to D11 (SPEC.md) -----

    @Test
    public void firstNumberAbove12_isReadDayFirst() {
        // D1: 13 can't be a month, so this date can be read only one way.
        assertOnly(DateCandidate.complete("BEST BEFORE 13/01/2027", "13/01/2027",
                DateLabel.BEST_BY, LocalDate.of(2027, 1, 13)), "BEST BEFORE 13/01/2027");
    }

    @Test
    public void sameDayAndMonth_isOneCompleteDate() {
        // D1: both readings are May 5, so there is nothing to choose between.
        assertOnly(DateCandidate.complete("USE BY 05/05/2027", "05/05/2027", DateLabel.USE_BY,
                LocalDate.of(2027, 5, 5)), "USE BY 05/05/2027");
    }

    @Test
    public void monthNames_areRead() {
        // D2
        LocalDate october12 = LocalDate.of(2026, 10, 12);

        assertOnly(DateCandidate.complete("BEST BEFORE 12 OCT 2026", "12 OCT 2026",
                DateLabel.BEST_BY, october12), "BEST BEFORE 12 OCT 2026");
        assertOnly(DateCandidate.complete("USE BY OCT 12, 2026", "OCT 12, 2026", DateLabel.USE_BY,
                october12), "USE BY OCT 12, 2026");
        assertOnly(DateCandidate.complete("BB 12OCT26", "12OCT26", DateLabel.BEST_BY, october12),
                "BB 12OCT26");
        assertOnly(DateCandidate.complete("Use by Oct. 12 26", "Oct. 12 26", DateLabel.USE_BY,
                october12), "Use by Oct. 12 26");
        assertOnly(DateCandidate.complete("BEST BY JUNE 1ST 2027", "JUNE 1ST 2027",
                DateLabel.BEST_BY, LocalDate.of(2027, 6, 1)), "BEST BY JUNE 1ST 2027");
        assertOnly(DateCandidate.complete("EXP 2027-MAR-15", "2027-MAR-15", DateLabel.EXPIRES,
                LocalDate.of(2027, 3, 15)), "EXP 2027-MAR-15");
    }

    @Test
    public void twoDigitYear_isReadAs20YY() {
        // D3
        assertOnly(DateCandidate.complete("BB 10/25/26", "10/25/26", DateLabel.BEST_BY,
                LocalDate.of(2026, 10, 25)), "BB 10/25/26");
        assertOnly(DateCandidate.twoReadings("EXP 03/04/27", "03/04/27", DateLabel.EXPIRES,
                LocalDate.of(2027, 3, 4), LocalDate.of(2027, 4, 3)), "EXP 03/04/27");
    }

    @Test
    public void lettersReadAsDigits_areFixed_butTheDateTextStaysAsPrinted() {
        // D4: the text reader read the letters O and I instead of the digits 0 and 1.
        assertOnly(DateCandidate.complete("EXP 1O/2I/2O26", "1O/2I/2O26", DateLabel.EXPIRES,
                LocalDate.of(2026, 10, 21)), "EXP 1O/2I/2O26");
    }

    @Test
    public void impossibleDates_areNotOffered() {
        // D5
        assertNoDates("BEST BY 02/30/2026");
        assertNoDates("BEST BY 13/13/2026");
        assertNoDates("EXP 13/2027");
    }

    @Test
    public void theLeapDay_isOnlyInALeapYear() {
        // D5
        assertNoDates("EXP 02/29/2027");
        assertOnly(DateCandidate.complete("EXP 02/29/2028", "02/29/2028", DateLabel.EXPIRES,
                LocalDate.of(2028, 2, 29)), "EXP 02/29/2028");
        // 29 can't be a month, so this can only be read day first.
        assertOnly(DateCandidate.complete("EXP 29/02/2028", "29/02/2028", DateLabel.EXPIRES,
                LocalDate.of(2028, 2, 29)), "EXP 29/02/2028");
    }

    @Test
    public void dotsDashesAndYearFirst_areRead() {
        // D6
        LocalDate october25 = LocalDate.of(2026, 10, 25);

        assertOnly(DateCandidate.complete("BEST BY 10.25.2026", "10.25.2026", DateLabel.BEST_BY,
                october25), "BEST BY 10.25.2026");
        assertOnly(DateCandidate.complete("BEST BY 10-25-2026", "10-25-2026", DateLabel.BEST_BY,
                october25), "BEST BY 10-25-2026");
        assertOnly(DateCandidate.complete("EXP 2027-03-15", "2027-03-15", DateLabel.EXPIRES,
                LocalDate.of(2027, 3, 15)), "EXP 2027-03-15");
    }

    @Test
    public void numbersWithOnlySpaces_areNotRead() {
        // D6: numbers with only spaces between them are too easy to confuse with other numbers.
        assertNoDates("BEST BY 10 25 2026");
    }

    @Test
    public void twoPartNumbers_areNotOffered() {
        // D7: "10/26" could be October 26 or October 2026. The parser never makes up the rest.
        assertNoDates("EXP 10/26");
        assertNoDates("BEST BY 03/27");
    }

    @Test
    public void labelWordsOnTheLineAbove_count() {
        // D8
        assertEquals(Collections.singletonList(DateCandidate.complete("10/25/2026", "10/25/2026",
                DateLabel.BEST_BY, LocalDate.of(2026, 10, 25))), find("BEST BY", "10/25/2026"));
    }

    @Test
    public void labelWordsTwoLinesAbove_dontCount() {
        // D8
        assertEquals(Collections.singletonList(DateCandidate.complete("10/25/2026", "10/25/2026",
                null, LocalDate.of(2026, 10, 25))),
                find("BEST BY", "HONEY OAT BARS", "10/25/2026"));
    }

    @Test
    public void labelWords_giveTheMatchingLabel() {
        // D9
        assertEquals(DateLabel.BEST_BY, labelOf("BEST IF USED BY 10/25/2026"));
        assertEquals(DateLabel.BEST_BY, labelOf("BBE 10/25/2026"));
        assertEquals(DateLabel.USE_BY, labelOf("USE OR FREEZE BY 10/25/2026"));
        assertEquals(DateLabel.USE_BY, labelOf("use before 10/25/2026"));
        assertEquals(DateLabel.EXPIRES, labelOf("EXPIRATION DATE 10/25/2026"));
        assertEquals(DateLabel.EXPIRES, labelOf("EXP.10/25/2026"));
        assertEquals(DateLabel.OTHER, labelOf("SELL BY 10/25/2026"));
        assertEquals(DateLabel.OTHER, labelOf("FREEZE BY 10/25/2026"));
    }

    @Test
    public void wordsThatOnlyContainLabelWords_areNotLabelWords() {
        // D9
        assertNull(labelOf("BBQ SAUCE 10/25/2026"));
        assertNull(labelOf("EXPRESS 10/25/2026"));
    }

    @Test
    public void madeAndPackedDates_areOfferedAsOther() {
        // D10: the date's button shows the line it came from, so the household can see "MFG".
        String line = "MFG 01/15/2025 EXP 01/15/2028";
        List<DateCandidate> expected = Arrays.asList(
                DateCandidate.complete(line, "01/15/2025", DateLabel.OTHER,
                        LocalDate.of(2025, 1, 15)),
                DateCandidate.complete(line, "01/15/2028", DateLabel.EXPIRES,
                        LocalDate.of(2028, 1, 15)));

        assertEquals(expected, find(line));
        assertOnly(DateCandidate.complete("PACKED 10/25/2026", "10/25/2026", DateLabel.OTHER,
                LocalDate.of(2026, 10, 25)), "PACKED 10/25/2026");
    }

    @Test
    public void theAnswerDoesNotDependOnToday() {
        // D11
        List<String> lines = Arrays.asList("BEST BY 10/25/2026", "EXP 03/04/2027", "EXP 03/2027");

        List<DateCandidate> now = parser.findCandidates(lines, TODAY);
        List<DateCandidate> later = parser.findCandidates(lines, LocalDate.of(2031, 1, 1));

        assertEquals(3, now.size());
        assertEquals(now, later);
    }

    @Test
    public void veryOldAndFarFutureDates_areStillOffered() {
        // D11: emergency supplies can sit for years, and some have very long dates.
        assertOnly(DateCandidate.complete("EXP 01/15/2019", "01/15/2019", DateLabel.EXPIRES,
                LocalDate.of(2019, 1, 15)), "EXP 01/15/2019");
        assertOnly(DateCandidate.monthAndYear("BEST BY 12/2050", "12/2050", DateLabel.BEST_BY,
                YearMonth.of(2050, 12)), "BEST BY 12/2050");
    }

    // ----- C. A made-up label (the household's real labels are not in these tests) -----

    @Test
    public void aMadeUpLabel_givesOnlyItsBestByDate() {
        List<DateCandidate> found = find("HONEY OAT GRANOLA BARS", "6 BARS NET WT 8.94 OZ (253g)",
                "BEST BY 10/25/2026", "LOT 2026-10-28");

        assertEquals(Collections.singletonList(DateCandidate.complete("BEST BY 10/25/2026",
                "10/25/2026", DateLabel.BEST_BY, LocalDate.of(2026, 10, 25))), found);
    }

    // ----- D. The answers fit the app -----

    @Test
    public void monthAndYearText_worksWithTheMonthEndButton() {
        // The form keeps a month-and-year answer's date text as the printed date, and its
        // "Use the last day of the month" button reads that text with PrintedMonthReader.
        String[] lines = {"EXP 03/2027", "EXP 2027-03", "EXP MAR 2027", "EXP 2027 MAR",
                "BEST BY 12/2050", "EXP O3/2O27"};
        for (String line : lines) {
            List<DateCandidate> found = find(line);
            assertEquals(line, 1, found.size());
            DateCandidate candidate = found.get(0);
            assertEquals(line, DateCandidate.Kind.MONTH_AND_YEAR, candidate.getKind());
            assertEquals(line, candidate.getMonth(),
                    PrintedMonthReader.printedMonth(candidate.getDateText()));
        }
        assertOnly(DateCandidate.monthAndYear("EXP O3/2O27", "O3/2O27", DateLabel.EXPIRES,
                YearMonth.of(2027, 3)), "EXP O3/2O27");
    }

    // ----- Helpers -----

    private List<DateCandidate> find(String... lines) {
        return parser.findCandidates(Arrays.asList(lines), TODAY);
    }

    /** Checks that this one line gives exactly this one candidate. */
    private void assertOnly(DateCandidate expected, String line) {
        assertEquals(line, Collections.singletonList(expected), find(line));
    }

    /** Checks that these lines, read together, give an empty list (never null). */
    private void assertNoDates(String... lines) {
        assertEquals(Arrays.toString(lines), Collections.emptyList(), find(lines));
    }

    /** The label of the only candidate in this line. */
    private DateLabel labelOf(String line) {
        List<DateCandidate> found = find(line);
        assertEquals(line, 1, found.size());
        return found.get(0).getLabel();
    }
}

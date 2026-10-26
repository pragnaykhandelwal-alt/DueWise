package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

/** Tests for how a date entry is recorded and when it shows "Date needs review". */
public class DateEntryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 26);

    @Test
    public void kindOf_completeDate_isComplete() {
        assertEquals(DateEntry.Kind.COMPLETE, DateEntry.kindOf(DATE, null));
    }

    @Test
    public void kindOf_printedTextOnly_isIncomplete() {
        assertEquals(DateEntry.Kind.INCOMPLETE, DateEntry.kindOf(null, "03/2027"));
    }

    @Test
    public void kindOf_nothingOrBlankText_isNone() {
        assertEquals(DateEntry.Kind.NONE, DateEntry.kindOf(null, null));
        assertEquals(DateEntry.Kind.NONE, DateEntry.kindOf(null, "   "));
    }

    @Test
    public void needsReview_onlyAConfirmedCompleteDateDoesNot() {
        assertFalse(DateEntry.needsReview(DATE, true));
        assertTrue(DateEntry.needsReview(DATE, false));
        assertTrue(DateEntry.needsReview(null, true));
        assertTrue(DateEntry.needsReview(null, false));
    }

    @Test
    public void needsReview_readsTheBatch() {
        BatchInfo incomplete = new BatchInfo(1, 1, "Rice", "", "", "bags", ItemCategory.FOOD, 2,
                null, "03/2027", DateLabel.EXPIRES, false, 1, "Pantry", LocationType.PANTRY,
                null, null, null, null, null, false, false);

        assertTrue(DateEntry.needsReview(incomplete));
        assertEquals(DateEntry.Kind.INCOMPLETE, DateEntry.kindOf(incomplete));
    }

    // ----- The date the "Use the last day of the month" button offers -----

    @Test
    public void lastDayOfPrintedMonth_monthAndYear_isTheLastDayOfThatMonth() {
        assertEquals(LocalDate.of(2027, 3, 31), DateEntry.lastDayOfPrintedMonth("03/2027"));
        assertEquals(LocalDate.of(2027, 4, 30), DateEntry.lastDayOfPrintedMonth("Apr 2027"));
        assertEquals(LocalDate.of(2026, 12, 31), DateEntry.lastDayOfPrintedMonth("2026-12"));
        assertEquals(LocalDate.of(2027, 3, 31), DateEntry.lastDayOfPrintedMonth("EXP 03/2027"));
    }

    @Test
    public void lastDayOfPrintedMonth_february_followsLeapYears() {
        assertEquals(LocalDate.of(2028, 2, 29), DateEntry.lastDayOfPrintedMonth("02/2028"));
        assertEquals(LocalDate.of(2027, 2, 28), DateEntry.lastDayOfPrintedMonth("02/2027"));
    }

    @Test
    public void lastDayOfPrintedMonth_february_centuryYears() {
        // Century years are leap years only when they can be divided by 400.
        assertEquals(LocalDate.of(2000, 2, 29), DateEntry.lastDayOfPrintedMonth("02/2000"));
        assertEquals(LocalDate.of(2100, 2, 28), DateEntry.lastDayOfPrintedMonth("02/2100"));
        assertEquals(LocalDate.of(1900, 2, 28), DateEntry.lastDayOfPrintedMonth("02/1900"));
    }

    @Test
    public void lastDayOfPrintedMonth_oldStock_isStillOffered() {
        assertEquals(LocalDate.of(2019, 6, 30), DateEntry.lastDayOfPrintedMonth("06/2019"));
    }

    @Test
    public void lastDayOfPrintedMonth_noSingleMonthAndYear_isNull() {
        assertNull(DateEntry.lastDayOfPrintedMonth(null));
        assertNull(DateEntry.lastDayOfPrintedMonth(""));
        assertNull(DateEntry.lastDayOfPrintedMonth("smudged"));
        assertNull(DateEntry.lastDayOfPrintedMonth("10/26"));           // month and year, or month and day?
        assertNull(DateEntry.lastDayOfPrintedMonth("2027"));            // no month
        assertNull(DateEntry.lastDayOfPrintedMonth("13/2027"));         // there is no 13th month
        assertNull(DateEntry.lastDayOfPrintedMonth("03/15/2027"));      // a full date
        assertNull(DateEntry.lastDayOfPrintedMonth("03/2027 04/2027")); // two months
    }

    @Test
    public void lastDayOfPrintedMonth_onlyYearsTheDatePickerShows() {
        assertNull(DateEntry.lastDayOfPrintedMonth("12/1899"));
        assertEquals(LocalDate.of(1900, 1, 31), DateEntry.lastDayOfPrintedMonth("01/1900"));
        assertEquals(LocalDate.of(2100, 12, 31), DateEntry.lastDayOfPrintedMonth("12/2100"));
        assertNull(DateEntry.lastDayOfPrintedMonth("01/2101"));
    }
}

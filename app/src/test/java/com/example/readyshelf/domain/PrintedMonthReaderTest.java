package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.YearMonth;

/**
 * Tests for reading one month and year from typed printed-date text (the form's "Use the last
 * day of the month" button). Until 2026-10-12 these were LabelDateFinderTest, which also tested
 * finding dates in label photos; those cases now check that such text is not one month and year.
 */
public class PrintedMonthReaderTest {

    private static final YearMonth MARCH_2027 = YearMonth.of(2027, 3);

    // ----- Text with only a month and year -----

    @Test
    public void printedMonth_readsTheMonthAndYear() {
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("03/2027"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("Mar 2027"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("2027-03"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXP 03/2027"));
    }

    @Test
    public void printedMonth_readsEveryMonthOnlyShape() {
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXP 2027-03"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXP MAR 2027"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXP 2027 MAR"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXP 03.2027"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXP 03-2027"));
    }

    @Test
    public void printedMonth_february2028_isFebruary2028() {
        assertEquals(YearMonth.of(2028, 2), PrintedMonthReader.printedMonth("EXP 02/2028"));
    }

    @Test
    public void printedMonth_acceptsAnyYear_becauseTheHouseholdTypedIt() {
        assertEquals(YearMonth.of(2015, 3), PrintedMonthReader.printedMonth("03/2015"));
        assertEquals(YearMonth.of(2040, 1), PrintedMonthReader.printedMonth("01/2040"));
    }

    @Test
    public void printedMonth_withLabelWords_stillReadsTheMonth() {
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("BEST IF USED BY 03/2027"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXPIRATION DATE MAR 2027"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("SELL BY 2027-03"));
    }

    @Test
    public void printedMonth_wordsThatOnlyContainALabelWord_changeNothing() {
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("BBQ 03/2027"));
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("LOTION 03/2027"));
    }

    @Test
    public void printedMonth_lettersReadInsteadOfDigits_areFixed() {
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("EXP O3/2O27"));
    }

    @Test
    public void printedMonth_theSameMonthTwice_isOneMonth() {
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("03/2027 03/2027"));
    }

    @Test
    public void printedMonth_aManufacturedMonthIsSkipped_soTheOtherOneCounts() {
        assertEquals(MARCH_2027, PrintedMonthReader.printedMonth("MFG 01/2025 EXP 03/2027"));
    }

    // ----- Text that is not exactly one month and year -----

    @Test
    public void printedMonth_otherText_isNull() {
        assertNull(PrintedMonthReader.printedMonth("10/26/2027")); // a full date
        assertNull(PrintedMonthReader.printedMonth("10/26"));
        assertNull(PrintedMonthReader.printedMonth("03/2027 04/2027"));
        assertNull(PrintedMonthReader.printedMonth("LOT 03/2027"));
        assertNull(PrintedMonthReader.printedMonth("hard to read"));
        assertNull(PrintedMonthReader.printedMonth(""));
    }

    @Test
    public void printedMonth_fullDatesInEveryShape_areNull() {
        assertNull(PrintedMonthReader.printedMonth("BEST BY 10/25/2026"));
        assertNull(PrintedMonthReader.printedMonth("EXP 04/05/2027")); // two readings
        assertNull(PrintedMonthReader.printedMonth("USE BY 05/05/2027"));
        assertNull(PrintedMonthReader.printedMonth("BEST BEFORE 13/01/2027"));
        assertNull(PrintedMonthReader.printedMonth("BB 10/25/26"));
        assertNull(PrintedMonthReader.printedMonth("BEST BY 10.25.2026"));
        assertNull(PrintedMonthReader.printedMonth("EXP 2027-03-15"));
        assertNull(PrintedMonthReader.printedMonth("EXP 2027-MAR-15"));
        assertNull(PrintedMonthReader.printedMonth("BEST BEFORE 12 OCT 2026"));
        assertNull(PrintedMonthReader.printedMonth("BB 12OCT26"));
        assertNull(PrintedMonthReader.printedMonth("USE BY OCT 12, 2026"));
        assertNull(PrintedMonthReader.printedMonth("BEST BY JUNE 1ST 2027"));
        assertNull(PrintedMonthReader.printedMonth("EXP 02/29/2028"));
    }

    @Test
    public void printedMonth_impossibleMonths_areNull() {
        assertNull(PrintedMonthReader.printedMonth("EXP 13/2027"));
        assertNull(PrintedMonthReader.printedMonth("EXP 00/2027"));
    }

    @Test
    public void printedMonth_skipWords_areNull() {
        assertNull(PrintedMonthReader.printedMonth("MFG 03/2027"));
        assertNull(PrintedMonthReader.printedMonth("LOT 2027-03"));
        assertNull(PrintedMonthReader.printedMonth("PACKED 03/2027"));
    }

    @Test
    public void printedMonth_numbersThatAreNotDates_areNull() {
        assertNull(PrintedMonthReader.printedMonth("2000 CALORIES A DAY"));
        assertNull(PrintedMonthReader.printedMonth("1/2 CUP (40g)"));
        assertNull(PrintedMonthReader.printedMonth("$10.26"));
        assertNull(PrintedMonthReader.printedMonth("BAKED AT 12:30"));
        assertNull(PrintedMonthReader.printedMonth("CALL 1-800-555-1234"));
        assertNull(PrintedMonthReader.printedMonth("NDC 0573-0164-40"));
    }

    // ----- OCR digit fixes -----

    @Test
    public void fixDigits_leavesMonthNamesAlone() {
        assertEquals("10/12/2026", PrintedMonthReader.fixDigits("1O/I2/2O26"));
        assertEquals("12OCT26", PrintedMonthReader.fixDigits("12OCT26"));
        assertEquals("OIL", PrintedMonthReader.fixDigits("OIL"));
    }
}

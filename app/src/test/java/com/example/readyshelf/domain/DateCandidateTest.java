package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Tests for the data type that the student's date-candidate parser returns. The type only holds
 * values; these tests check that, and are not tests of the parser's rules.
 */
public class DateCandidateTest {

    private static final LocalDate MARCH_4 = LocalDate.of(2027, 3, 4);
    private static final LocalDate APRIL_3 = LocalDate.of(2027, 4, 3);

    @Test
    public void complete_holdsItsValues_andTheOtherKindsAreNull() {
        DateCandidate candidate = DateCandidate.complete("BEST BY 10/26/2027", "10/26/2027",
                DateLabel.BEST_BY, LocalDate.of(2027, 10, 26));

        assertEquals(DateCandidate.Kind.COMPLETE, candidate.getKind());
        assertEquals("BEST BY 10/26/2027", candidate.getRawText());
        assertEquals("10/26/2027", candidate.getDateText());
        assertEquals(DateLabel.BEST_BY, candidate.getLabel());
        assertEquals(LocalDate.of(2027, 10, 26), candidate.getDate());
        assertNull(candidate.getMonthFirst());
        assertNull(candidate.getDayFirst());
        assertNull(candidate.getMonth());
    }

    @Test
    public void twoReadings_keepsBothDates() {
        DateCandidate candidate = DateCandidate.twoReadings("EXP 03/04/27", "03/04/27",
                DateLabel.EXPIRES, MARCH_4, APRIL_3);

        assertEquals(DateCandidate.Kind.TWO_READINGS, candidate.getKind());
        assertEquals(MARCH_4, candidate.getMonthFirst());
        assertEquals(APRIL_3, candidate.getDayFirst());
        assertNull(candidate.getDate());
        assertNull(candidate.getMonth());
    }

    @Test
    public void monthAndYear_hasNoDay() {
        DateCandidate candidate = DateCandidate.monthAndYear("EXP 03/2027", "03/2027",
                DateLabel.EXPIRES, YearMonth.of(2027, 3));

        assertEquals(DateCandidate.Kind.MONTH_AND_YEAR, candidate.getKind());
        assertEquals(YearMonth.of(2027, 3), candidate.getMonth());
        assertNull(candidate.getDate());
        assertNull(candidate.getMonthFirst());
        assertNull(candidate.getDayFirst());
    }

    @Test
    public void theLabelCanBeNull() {
        assertNull(DateCandidate.complete("10/26/2027", "10/26/2027", null,
                LocalDate.of(2027, 10, 26)).getLabel());
    }

    @Test(expected = NullPointerException.class)
    public void complete_needsADate() {
        DateCandidate.complete("10/26/2027", "10/26/2027", null, null);
    }

    @Test(expected = NullPointerException.class)
    public void twoReadings_needsBothDates() {
        DateCandidate.twoReadings("03/04/27", "03/04/27", null, MARCH_4, null);
    }

    @Test(expected = NullPointerException.class)
    public void monthAndYear_needsAMonth() {
        DateCandidate.monthAndYear("03/2027", "03/2027", null, null);
    }

    @Test(expected = NullPointerException.class)
    public void theRawTextIsNeeded() {
        DateCandidate.complete(null, "10/26/2027", null, LocalDate.of(2027, 10, 26));
    }

    @Test
    public void equals_comparesEveryValue() {
        DateCandidate one = DateCandidate.twoReadings("EXP 03/04/27", "03/04/27", DateLabel.EXPIRES,
                MARCH_4, APRIL_3);

        assertEquals(one, DateCandidate.twoReadings("EXP 03/04/27", "03/04/27", DateLabel.EXPIRES,
                MARCH_4, APRIL_3));
        assertEquals(one.hashCode(), DateCandidate.twoReadings("EXP 03/04/27", "03/04/27",
                DateLabel.EXPIRES, MARCH_4, APRIL_3).hashCode());
        assertNotEquals(one, DateCandidate.twoReadings("EXP  03/04/27", "03/04/27",
                DateLabel.EXPIRES, MARCH_4, APRIL_3));
        assertNotEquals(one, DateCandidate.twoReadings("EXP 03/04/27", "03/04/27", null,
                MARCH_4, APRIL_3));
        assertNotEquals(one, DateCandidate.twoReadings("EXP 03/04/27", "03/04/27", DateLabel.EXPIRES,
                APRIL_3, MARCH_4));
        assertNotEquals(one, DateCandidate.complete("EXP 03/04/27", "03/04/27", DateLabel.EXPIRES,
                MARCH_4));
    }

    @Test
    public void toString_showsTheKindAndTheRawText() {
        String text = DateCandidate.monthAndYear("EXP 03/2027", "03/2027", null,
                YearMonth.of(2027, 3)).toString();

        assertTrue(text.contains("MONTH_AND_YEAR"));
        assertTrue(text.contains("EXP 03/2027"));
    }

    @Test
    public void survivesBeingSaved() throws IOException, ClassNotFoundException {
        DateCandidate candidate = DateCandidate.twoReadings("EXP 03/04/27", "03/04/27",
                DateLabel.EXPIRES, MARCH_4, APRIL_3);

        assertEquals(candidate, roundTrip(candidate));
    }

    /** Writes and reads an object the way Android keeps it when it closes the app. */
    static Object roundTrip(Object value) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }
}

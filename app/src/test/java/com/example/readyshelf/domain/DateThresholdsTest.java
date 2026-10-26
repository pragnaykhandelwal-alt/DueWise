package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests for storing and checking the Settings numbers. */
public class DateThresholdsTest {

    @Test
    public void defaults_are30NearDateDaysAnd180ReplacementDays() {
        DateThresholds thresholds = DateThresholds.defaults();

        assertEquals(30, thresholds.getNearDateDays());
        assertEquals(180, thresholds.getReplacementDays());
    }

    @Test
    public void of_keepsBothNumbers() {
        DateThresholds thresholds = DateThresholds.of(7, 365);

        assertEquals(7, thresholds.getNearDateDays());
        assertEquals(365, thresholds.getReplacementDays());
    }

    @Test
    public void isValidNearDateDays_allows1To365() {
        assertTrue(DateThresholds.isValidNearDateDays(1));
        assertTrue(DateThresholds.isValidNearDateDays(365));
        assertFalse(DateThresholds.isValidNearDateDays(0));
        assertFalse(DateThresholds.isValidNearDateDays(366));
    }

    @Test
    public void isValidReplacementDays_allows1To3650() {
        assertTrue(DateThresholds.isValidReplacementDays(1));
        assertTrue(DateThresholds.isValidReplacementDays(3650));
        assertFalse(DateThresholds.isValidReplacementDays(0));
        assertFalse(DateThresholds.isValidReplacementDays(3651));
    }

    @Test
    public void of_rejectsNumbersOutsideTheRanges() {
        assertThrows(IllegalArgumentException.class, () -> DateThresholds.of(0, 180));
        assertThrows(IllegalArgumentException.class, () -> DateThresholds.of(366, 180));
        assertThrows(IllegalArgumentException.class, () -> DateThresholds.of(30, 0));
        assertThrows(IllegalArgumentException.class, () -> DateThresholds.of(30, 3651));
    }

    @Test
    public void of_rejectsReplacementDaysThatAreNotMoreThanTheNearDateDays() {
        assertThrows(IllegalArgumentException.class, () -> DateThresholds.of(30, 30));
        assertThrows(IllegalArgumentException.class, () -> DateThresholds.of(31, 30));
        assertThrows(IllegalArgumentException.class, () -> DateThresholds.of(365, 365));
    }

    @Test
    public void of_acceptsReplacementDaysOneMoreThanTheNearDateDays() {
        assertEquals(31, DateThresholds.of(30, 31).getReplacementDays());
        assertEquals(366, DateThresholds.of(365, 366).getReplacementDays());
    }

    @Test
    public void isValidPair_needsMoreReplacementDaysThanNearDateDays() {
        assertTrue(DateThresholds.isValidPair(30, 180));
        assertTrue(DateThresholds.isValidPair(30, 31));
        assertFalse(DateThresholds.isValidPair(30, 30));
        assertFalse(DateThresholds.isValidPair(200, 180));
    }

    @Test
    public void fromSaved_keepsAValidPair() {
        assertEquals(DateThresholds.of(45, 200), DateThresholds.fromSaved(45, 200));
    }

    @Test
    public void fromSaved_givesBothStartingValues_whenAnOlderSavedPairBreaksTheRule() {
        // Before 2026-10-11 the app allowed saving 200 near-date days and 180 replacement days.
        assertEquals(DateThresholds.defaults(), DateThresholds.fromSaved(200, 180));
        assertEquals(DateThresholds.defaults(), DateThresholds.fromSaved(30, 30));
    }

    @Test
    public void fromSaved_repairsEachNumberOutsideItsRange() {
        assertEquals(DateThresholds.of(30, 400), DateThresholds.fromSaved(0, 400));
        assertEquals(DateThresholds.of(20, 180), DateThresholds.fromSaved(20, 9999));
        assertEquals(DateThresholds.defaults(), DateThresholds.fromSaved(0, 9999));
        // 400 near-date days is out of range (becomes 30); 20 replacement days is then too few.
        assertEquals(DateThresholds.defaults(), DateThresholds.fromSaved(400, 20));
    }

    @Test
    public void equals_comparesBothNumbers() {
        assertEquals(DateThresholds.of(30, 180), DateThresholds.defaults());
        assertNotEquals(DateThresholds.of(30, 181), DateThresholds.defaults());
        assertNotEquals(DateThresholds.of(31, 180), DateThresholds.defaults());
    }
}

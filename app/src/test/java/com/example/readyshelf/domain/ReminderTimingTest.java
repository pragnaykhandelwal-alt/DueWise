package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Tests for ReminderTiming: the wait before the first daily reminder check. */
public class ReminderTimingTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    public void beforeNine_waitsUntilNineTheSameDay() {
        assertEquals(Duration.ofHours(1), delayFrom(2026, 10, 11, 8, 0));
    }

    @Test
    public void afterNine_waitsUntilNineTheNextDay() {
        assertEquals(Duration.ofMinutes(9 * 60 + 30), delayFrom(2026, 10, 11, 23, 30));
        assertEquals(Duration.ofHours(23).plusMinutes(59), delayFrom(2026, 10, 11, 9, 1));
    }

    @Test
    public void exactlyAtNine_waitsAWholeDay() {
        assertEquals(Duration.ofHours(24), delayFrom(2026, 10, 11, 9, 0));
    }

    @Test
    public void whenClocksMoveForward_theNightIsAnHourShorter() {
        // In New York, clocks moved from 2:00 to 3:00 on 8 March 2026.
        assertEquals(Duration.ofHours(10), delayFrom(2026, 3, 7, 22, 0));
    }

    @Test
    public void whenClocksMoveBack_theNightIsAnHourLonger() {
        // In New York, clocks move from 2:00 back to 1:00 on 1 November 2026.
        assertEquals(Duration.ofHours(12), delayFrom(2026, 10, 31, 22, 0));
    }

    private static Duration delayFrom(int year, int month, int day, int hour, int minute) {
        return ReminderTiming.firstRunDelay(
                ZonedDateTime.of(year, month, day, hour, minute, 0, 0, NEW_YORK));
    }
}

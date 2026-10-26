package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * When the first daily reminder check may run. This is plain Java, so it can be tested without
 * a phone.
 *
 * <p>The first check waits until the next 9:00 in the morning (local time), so turning reminders
 * on in the evening doesn't lead to a check at night. This is only a starting point: Android
 * runs each later check about a day after the one before, and it can delay checks to save
 * battery. That is why the app never promises a time.
 */
public final class ReminderTiming {

    /** The local time the first check aims for. */
    public static final LocalTime FIRST_CHECK = LocalTime.of(9, 0);

    private ReminderTiming() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * How long to wait before the first check: until the next 9:00 after {@code now}, in
     * {@code now}'s time zone. Exactly at 9:00 it waits a whole day. Days on which the clocks
     * change are 23 or 25 hours long, and the result follows the real clock.
     */
    @NonNull
    public static Duration firstRunDelay(@NonNull ZonedDateTime now) {
        ZonedDateTime next = now.with(FIRST_CHECK);
        if (!next.isAfter(now)) {
            next = next.plusDays(1).with(FIRST_CHECK);
        }
        return Duration.between(now, next);
    }
}

package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * The household's planning settings that the rules use. An object of this class never changes.
 * <ul>
 *   <li><b>Near-date days</b> (starts at 30): how many days ahead a date counts as near.</li>
 *   <li><b>Replacement days</b> (starts at 180): how far from today a replacement's date must
 *       be, at least. It must be more than the near-date days. This is a planning choice, not a
 *       safety guarantee.</li>
 * </ul>
 * The app passes these numbers to the rules as parameters, so a changed setting takes effect
 * without any change to the rules.
 */
public final class DateThresholds {

    public static final int DEFAULT_NEAR_DATE_DAYS = 30;
    public static final int DEFAULT_REPLACEMENT_DAYS = 180;
    public static final int MIN_DAYS = 1;
    public static final int MAX_NEAR_DATE_DAYS = 365;
    public static final int MAX_REPLACEMENT_DAYS = 3650;

    private final int nearDateDays;
    private final int replacementDays;

    private DateThresholds(int nearDateDays, int replacementDays) {
        if (!isValidNearDateDays(nearDateDays)) {
            throw new IllegalArgumentException("Near-date days must be from " + MIN_DAYS + " to "
                    + MAX_NEAR_DATE_DAYS + ", not " + nearDateDays);
        }
        if (!isValidReplacementDays(replacementDays)) {
            throw new IllegalArgumentException("Replacement days must be from " + MIN_DAYS + " to "
                    + MAX_REPLACEMENT_DAYS + ", not " + replacementDays);
        }
        if (!isValidPair(nearDateDays, replacementDays)) {
            throw new IllegalArgumentException("Replacement days (" + replacementDays
                    + ") must be more than the near-date days (" + nearDateDays + ")");
        }
        this.nearDateDays = nearDateDays;
        this.replacementDays = replacementDays;
    }

    /** The starting numbers: 30 near-date days and 180 replacement days. */
    @NonNull
    public static DateThresholds defaults() {
        return new DateThresholds(DEFAULT_NEAR_DATE_DAYS, DEFAULT_REPLACEMENT_DAYS);
    }

    /**
     * The given numbers.
     *
     * @throws IllegalArgumentException when a number is outside its allowed range, or when the
     *                                  replacement days are not more than the near-date days
     */
    @NonNull
    public static DateThresholds of(int nearDateDays, int replacementDays) {
        return new DateThresholds(nearDateDays, replacementDays);
    }

    /**
     * Numbers read back from the phone's storage, made usable. A number outside its range gets
     * its starting value. If the replacement days are then not more than the near-date days (an
     * older version of the app allowed that), both numbers get their starting values (30 and
     * 180), so a saved pair can never stop the app from starting.
     */
    @NonNull
    public static DateThresholds fromSaved(int nearDateDays, int replacementDays) {
        int near = isValidNearDateDays(nearDateDays) ? nearDateDays : DEFAULT_NEAR_DATE_DAYS;
        int replacement = isValidReplacementDays(replacementDays) ? replacementDays
                : DEFAULT_REPLACEMENT_DAYS;
        return isValidPair(near, replacement) ? new DateThresholds(near, replacement) : defaults();
    }

    /** True when {@code days} is allowed as near-date days: from 1 to 365. */
    public static boolean isValidNearDateDays(int days) {
        return days >= MIN_DAYS && days <= MAX_NEAR_DATE_DAYS;
    }

    /** True when {@code days} is allowed as replacement days: from 1 to 3650. */
    public static boolean isValidReplacementDays(int days) {
        return days >= MIN_DAYS && days <= MAX_REPLACEMENT_DAYS;
    }

    /** True when the replacement days are more than the near-date days. */
    public static boolean isValidPair(int nearDateDays, int replacementDays) {
        return replacementDays > nearDateDays;
    }

    public int getNearDateDays() {
        return nearDateDays;
    }

    public int getReplacementDays() {
        return replacementDays;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DateThresholds)) {
            return false;
        }
        DateThresholds that = (DateThresholds) other;
        return nearDateDays == that.nearDateDays && replacementDays == that.replacementDays;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nearDateDays, replacementDays);
    }

    @NonNull
    @Override
    public String toString() {
        return "DateThresholds{nearDateDays=" + nearDateDays + ", replacementDays=" + replacementDays + "}";
    }
}

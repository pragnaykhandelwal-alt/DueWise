package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Counts the batches on Home's review lists by status, for the "Items need review" banner at the
 * top of Home. This is plain Java, so it can be tested without a phone.
 *
 * <p>It makes no date decisions: every status comes from the student's date rules (DatePolicy),
 * through {@link RuleRunner#needsReview}. Every item of the list counts (medicine, sample items,
 * and batches with nothing left too), because Home lists them all. Only the daily reminder leaves
 * some of them out (see ReminderCheck).
 *
 * <p>Since 2026-10-23 (the student's choice) {@link #tilesTotal} adds up Home's status tiles. That
 * one number goes on the app icon, with the reminder, and on the home-screen widget.
 */
public final class ReviewCounts {

    /**
     * The banner's order: "Near date" first and "Date passed" third (since 2026-10-25, the
     * student's choice; until then the most urgent status came first). Home never lists "Later
     * date".
     */
    private static final List<DateStatus> ORDER = Collections.unmodifiableList(Arrays.asList(
            DateStatus.NEAR_DATE, DateStatus.TODAY, DateStatus.PAST_DATE,
            DateStatus.NEEDS_REVIEW));

    /** One status, and how many items of a review list have it. Never changes. */
    public static final class StatusCount {

        @NonNull private final DateStatus status;
        private final int count;

        public StatusCount(@NonNull DateStatus status, int count) {
            this.status = Objects.requireNonNull(status, "status");
            this.count = count;
        }

        @NonNull
        public DateStatus getStatus() {
            return status;
        }

        public int getCount() {
            return count;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof StatusCount)) {
                return false;
            }
            StatusCount that = (StatusCount) other;
            return status == that.status && count == that.count;
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, count);
        }

        @NonNull
        @Override
        public String toString() {
            return "StatusCount{" + status + ", count=" + count + "}";
        }
    }

    private ReviewCounts() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * How many items of a review list have each status, in the banner's order: "Near date",
     * "Date is today", "Date passed", then "Date needs review". A status that no item has is left
     * out, so an empty result means that the banner stays hidden. The result can't be changed.
     */
    @NonNull
    public static List<StatusCount> byStatus(@NonNull List<BatchWithStatus> reviewList) {
        List<StatusCount> result = new ArrayList<>();
        for (DateStatus status : ORDER) {
            int count = 0;
            for (BatchWithStatus item : reviewList) {
                if (item.getStatus() == status) {
                    count++;
                }
            }
            if (count > 0) {
                result.add(new StatusCount(status, count));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * True for the statuses that get a tile on Home: "Date passed", "Date is today", and "Near
     * date". "Date needs review" gets no tile (the student's choice on 2026-10-19), and Home never
     * lists "Later date".
     */
    public static boolean hasTile(@NonNull DateStatus status) {
        return status == DateStatus.PAST_DATE || status == DateStatus.TODAY
                || status == DateStatus.NEAR_DATE;
    }

    /**
     * Home's status tiles added up: how many items of a review list have a status with a tile
     * (see {@link #hasTile}). Medicine, sample items, and batches with nothing left count too, like
     * on the tiles. Since 2026-10-23 the app icon (with the reminder) and the home-screen widget
     * show this number (the student's choice).
     */
    public static int tilesTotal(@NonNull List<BatchWithStatus> reviewList) {
        int total = 0;
        for (BatchWithStatus item : reviewList) {
            if (hasTile(item.getStatus())) {
                total++;
            }
        }
        return total;
    }
}

package com.example.readyshelf.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.BatchWithStatus;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.ReviewCounts;
import com.example.readyshelf.domain.RuleRunner;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * What the home-screen widget says. This is plain Java, so it can be tested without a phone.
 *
 * <p>The widget looks at the same review list as Home: every status from the student's date rules
 * (DatePolicy) except "Later date". Unlike the daily reminder, "Date needs review" items, sample
 * items in the household's records, and batches with nothing left count too, because Home lists
 * them. The widget never names items.
 *
 * <p>Since 2026-10-23 (the student's choice) it also gives one number: Home's status tiles added up
 * ({@link ReviewCounts#tilesTotal}), the same number as on the app icon. Until then it never gave
 * counts, and this class was an enum of the three kinds below. Never changes.
 */
public final class ReviewWidgetState {

    /** The three kinds of line the widget can show. */
    public enum Kind {
        /** "5 household items need review": Home's review list has at least one item. */
        ITEMS_TO_REVIEW,
        /** "Nothing needs review right now.": Home's review list is empty. */
        NOTHING_TO_REVIEW,
        /**
         * "Open DueWise to see the review list.": the answer isn't known, because the date rules
         * are not connected or stopped with an error, or the widget wasn't refreshed yet. These
         * words are true in every case, so the widget never shows a wrong "Nothing needs review".
         */
        UNKNOWN
    }

    /** Home's review list is empty. */
    public static final ReviewWidgetState NOTHING_TO_REVIEW =
            new ReviewWidgetState(Kind.NOTHING_TO_REVIEW, 0);
    /** The answer isn't known (see {@link Kind#UNKNOWN}). */
    public static final ReviewWidgetState UNKNOWN = new ReviewWidgetState(Kind.UNKNOWN, 0);

    @NonNull private final Kind kind;
    private final int number;

    private ReviewWidgetState(@NonNull Kind kind, int number) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.number = number;
    }

    /**
     * Home's review list has at least one item, and {@code number} of them have a status with a
     * tile on Home. The number is 0 when every item only has a date to review.
     */
    @NonNull
    public static ReviewWidgetState itemsToReview(int number) {
        return new ReviewWidgetState(Kind.ITEMS_TO_REVIEW, number);
    }

    /**
     * Works out what the widget says. The batches are read only when the date rules are
     * connected.
     *
     * @param batches gives the household's batches that are not archived
     * @throws RuntimeException when the date rules throw; the caller decides what to show
     */
    @NonNull
    public static ReviewWidgetState check(@Nullable DatePolicyContract policy,
                                          @NonNull Supplier<List<BatchInfo>> batches,
                                          @NonNull LocalDate today,
                                          @NonNull DateThresholds thresholds) {
        if (policy == null) {
            return UNKNOWN;
        }
        List<BatchWithStatus> reviewList =
                RuleRunner.needsReview(batches.get(), policy, today, thresholds);
        return reviewList.isEmpty() ? NOTHING_TO_REVIEW
                : itemsToReview(ReviewCounts.tilesTotal(reviewList));
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    /** Home's status tiles added up. Always 0 unless the kind is ITEMS_TO_REVIEW. */
    public int getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReviewWidgetState)) {
            return false;
        }
        ReviewWidgetState that = (ReviewWidgetState) other;
        return kind == that.kind && number == that.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, number);
    }

    @NonNull
    @Override
    public String toString() {
        return "ReviewWidgetState{" + kind + ", number=" + number + "}";
    }
}

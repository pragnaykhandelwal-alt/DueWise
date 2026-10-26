package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * What the reminder lists (added 2026-10-19, the student's choice): up to two entries, and how
 * many others there are. The notification shows it as, for example, "Canned beans, Granola bars,
 * and 3 others" ("and 3 more" until 2026-10-23). Settings' "Show reminder now" shows the same
 * list. This is plain Java, so it can be tested without a phone.
 *
 * <p>Only items that cause the reminder count (see {@link ReminderCheck#causesReminder}). They
 * keep the review list's order: soonest date first, like Home. An entry is a product name, as the
 * household typed it. Batches with the same name are one entry (capital letters and spaces around
 * the name don't matter), so a name is never listed twice. Medicine is never named: all medicine
 * together is one entry, which the notification calls "medicine".
 *
 * <p>Since 2026-10-23 (the student's choice) the list also carries the number for the app icon:
 * Home's status tiles added up ({@link ReviewCounts#tilesTotal}). It counts what the tiles count,
 * batch by batch, so it can be larger than the list: medicine batches, several batches with one
 * name, sample items, and batches with nothing left all count. It can even be above 0 when nothing
 * causes a reminder (for example only old sample items); then no reminder carries it.
 */
public final class ReminderList {

    /** How many entries the reminder shows. The others are only counted. */
    public static final int SHOWN_ENTRIES = 2;

    /** The list when nothing causes a reminder. */
    public static final ReminderList EMPTY = new ReminderList(Collections.emptyList(), 0, 0);

    /** One entry of the list: a product name, or all medicine together. Never changes. */
    public static final class Entry {

        /** All medicine batches together. Their names never appear in a notification. */
        public static final Entry MEDICINE = new Entry(null);

        /** The product name, or null for the medicine entry. */
        @Nullable private final String name;

        private Entry(@Nullable String name) {
            this.name = name;
        }

        /** The entry of a product that is not medicine. */
        @NonNull
        public static Entry named(@NonNull String name) {
            return new Entry(Objects.requireNonNull(name, "name"));
        }

        /** True for the medicine entry, which has no name. */
        public boolean isMedicine() {
            return name == null;
        }

        /** The product name. Empty for the medicine entry. */
        @NonNull
        public String getName() {
            return name == null ? "" : name;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Entry)) {
                return false;
            }
            return Objects.equals(name, ((Entry) other).name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        @NonNull
        @Override
        public String toString() {
            return name == null ? "Entry{medicine}" : "Entry{" + name + "}";
        }
    }

    @NonNull private final List<Entry> shown;
    private final int moreCount;
    private final int iconNumber;

    private ReminderList(@NonNull List<Entry> shown, int moreCount, int iconNumber) {
        this.shown = Collections.unmodifiableList(new ArrayList<>(shown));
        this.moreCount = moreCount;
        this.iconNumber = iconNumber;
    }

    /**
     * The reminder's list for a review list from {@link RuleRunner#needsReview}, which is sorted
     * soonest date first. Items that don't cause a reminder are left out.
     */
    @NonNull
    public static ReminderList of(@NonNull List<BatchWithStatus> reviewList) {
        List<Entry> entries = new ArrayList<>();
        // A set that compares names without capital letters: "Canned beans" and "canned beans"
        // count as the same name.
        Set<String> namesListed = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        boolean medicineListed = false;
        for (BatchWithStatus item : reviewList) {
            if (!ReminderCheck.causesReminder(item)) {
                continue;
            }
            BatchInfo batch = item.getBatch();
            if (batch.getCategory() == ItemCategory.MEDICINE) {
                if (!medicineListed) {
                    medicineListed = true;
                    entries.add(Entry.MEDICINE);
                }
            } else {
                String name = batch.getName().trim();
                // add() returns false when the set already has this name.
                if (namesListed.add(name)) {
                    entries.add(Entry.named(name));
                }
            }
        }
        int shownCount = Math.min(entries.size(), SHOWN_ENTRIES);
        return new ReminderList(entries.subList(0, shownCount), entries.size() - shownCount,
                ReviewCounts.tilesTotal(reviewList));
    }

    /** The entries the reminder shows, at most two, in order. The list can't be changed. */
    @NonNull
    public List<Entry> getShown() {
        return shown;
    }

    /** How many other entries there are: the 3 of "and 3 others". */
    public int getMoreCount() {
        return moreCount;
    }

    /**
     * The number for the app icon: Home's status tiles added up, for the same review list. The
     * reminder carries it (Notification.number), so a home screen that shows numbers shows it on
     * DueWise's icon; others show a dot.
     */
    public int getIconNumber() {
        return iconNumber;
    }

    /** True when nothing causes a reminder. */
    public boolean isEmpty() {
        return shown.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReminderList)) {
            return false;
        }
        ReminderList that = (ReminderList) other;
        return moreCount == that.moreCount && iconNumber == that.iconNumber
                && shown.equals(that.shown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shown, moreCount, iconNumber);
    }

    @NonNull
    @Override
    public String toString() {
        return "ReminderList{" + shown + ", more=" + moreCount + ", icon=" + iconNumber + "}";
    }
}

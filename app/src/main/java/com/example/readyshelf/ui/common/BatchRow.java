package com.example.readyshelf.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.BatchWithStatus;
import com.example.readyshelf.domain.DateStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One row in a batch list: a batch, its status when the date rules are connected, an optional
 * note line, and up to two buttons (the Purchase/Renewal list uses them).
 */
public final class BatchRow {

    /**
     * A button at the end of a row's card, for example "Add to purchase list". The screen that
     * gives rows an action also tells BatchAdapter what a tap on it does. Never changes.
     */
    public static final class Action {
        @NonNull private final String text;
        @NonNull private final String description;
        private final boolean enabled;

        /**
         * @param text        the button's words
         * @param description what TalkBack reads for it, for example with the product's name
         * @param enabled     false while the screen is saving a change
         */
        public Action(@NonNull String text, @NonNull String description, boolean enabled) {
            this.text = Objects.requireNonNull(text, "text");
            this.description = Objects.requireNonNull(description, "description");
            this.enabled = enabled;
        }

        @NonNull
        public String getText() {
            return text;
        }

        @NonNull
        public String getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Action)) {
                return false;
            }
            Action that = (Action) other;
            return enabled == that.enabled && text.equals(that.text)
                    && description.equals(that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, description, enabled);
        }
    }

    @NonNull private final BatchInfo batch;
    @Nullable private final DateStatus status;
    @Nullable private final String note;
    @Nullable private final Action action;
    // A second button under the first, for example "Discard" under "Renewed" (since 2026-10-24).
    @Nullable private final Action secondAction;

    public BatchRow(@NonNull BatchInfo batch, @Nullable DateStatus status) {
        this(batch, status, null);
    }

    public BatchRow(@NonNull BatchInfo batch, @Nullable DateStatus status, @Nullable String note) {
        this(batch, status, note, null);
    }

    public BatchRow(@NonNull BatchInfo batch, @Nullable DateStatus status, @Nullable String note,
                    @Nullable Action action) {
        this(batch, status, note, action, null);
    }

    public BatchRow(@NonNull BatchInfo batch, @Nullable DateStatus status, @Nullable String note,
                    @Nullable Action action, @Nullable Action secondAction) {
        this.batch = Objects.requireNonNull(batch, "batch");
        this.status = status;
        this.note = note;
        this.action = action;
        this.secondAction = secondAction;
    }

    /** Rows without a status, for when the date rules are not connected. */
    @NonNull
    public static List<BatchRow> withoutStatus(@NonNull List<BatchInfo> batches) {
        List<BatchRow> rows = new ArrayList<>();
        for (BatchInfo batch : batches) {
            rows.add(new BatchRow(batch, null));
        }
        return rows;
    }

    /** Rows that show the status the date rules gave each batch. */
    @NonNull
    public static List<BatchRow> withStatus(@NonNull List<BatchWithStatus> batches) {
        List<BatchRow> rows = new ArrayList<>();
        for (BatchWithStatus item : batches) {
            rows.add(new BatchRow(item.getBatch(), item.getStatus()));
        }
        return rows;
    }

    @NonNull
    public BatchInfo getBatch() {
        return batch;
    }

    /** The status, or null when the date rules are not connected. */
    @Nullable
    public DateStatus getStatus() {
        return status;
    }

    /** An extra line under the row, or null for none. */
    @Nullable
    public String getNote() {
        return note;
    }

    /** The button at the end of the row, or null for none. */
    @Nullable
    public Action getAction() {
        return action;
    }

    /** The button under the first one, or null for none. */
    @Nullable
    public Action getSecondAction() {
        return secondAction;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BatchRow)) {
            return false;
        }
        BatchRow that = (BatchRow) other;
        return batch.equals(that.batch) && status == that.status && Objects.equals(note, that.note)
                && Objects.equals(action, that.action)
                && Objects.equals(secondAction, that.secondAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batch, status, note, action, secondAction);
    }
}

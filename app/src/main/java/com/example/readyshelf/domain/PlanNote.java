package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * What a swap plan says about one batch: a reason, and how many units of the batch still need
 * replacement (0 when none). The swap rules (the student's RotationPlanner) make these.
 * An object of this class never changes.
 */
public final class PlanNote {

    private final long batchId;
    @NonNull private final PlanReason reason;
    private final int stillNeeded;

    public PlanNote(long batchId, @NonNull PlanReason reason, int stillNeeded) {
        this.batchId = batchId;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.stillNeeded = stillNeeded;
    }

    public long getBatchId() {
        return batchId;
    }

    @NonNull
    public PlanReason getReason() {
        return reason;
    }

    /** How many units of this batch still need replacement. The Purchase list shows them. */
    public int getStillNeeded() {
        return stillNeeded;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlanNote)) {
            return false;
        }
        PlanNote that = (PlanNote) other;
        return batchId == that.batchId && reason == that.reason && stillNeeded == that.stillNeeded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, reason, stillNeeded);
    }

    @NonNull
    @Override
    public String toString() {
        return "PlanNote{batch=" + batchId + ", " + reason + ", stillNeeded=" + stillNeeded + "}";
    }
}

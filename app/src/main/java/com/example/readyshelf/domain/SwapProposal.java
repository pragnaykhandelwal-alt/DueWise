package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * One proposed exchange: {@code quantity} units of the kit batch go to the pantry, and the
 * same number of units of the pantry batch go into the kit. The swap rules (the student's
 * RotationPlanner) make these. An object of this class never changes.
 *
 * <p>A proposal changes nothing that is stored. The household decides whether to do it.
 */
public final class SwapProposal {

    private final long kitBatchId;
    private final long pantryBatchId;
    private final int quantity;

    public SwapProposal(long kitBatchId, long pantryBatchId, int quantity) {
        this.kitBatchId = kitBatchId;
        this.pantryBatchId = pantryBatchId;
        this.quantity = quantity;
    }

    /** The kit batch whose units would move to the pantry. */
    public long getKitBatchId() {
        return kitBatchId;
    }

    /** The pantry batch whose units would move into the kit. */
    public long getPantryBatchId() {
        return pantryBatchId;
    }

    /** How many units move each way. */
    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SwapProposal)) {
            return false;
        }
        SwapProposal that = (SwapProposal) other;
        return kitBatchId == that.kitBatchId && pantryBatchId == that.pantryBatchId
                && quantity == that.quantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kitBatchId, pantryBatchId, quantity);
    }

    @NonNull
    @Override
    public String toString() {
        return "SwapProposal{kit=" + kitBatchId + ", pantry=" + pantryBatchId
                + ", quantity=" + quantity + "}";
    }
}

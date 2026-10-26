package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The result of the swap rules (the student's RotationPlanner): proposed swaps and notes.
 * An object of this class never changes, and its lists can't be changed.
 *
 * <p>A plan is only a proposal. Making a plan changes nothing that is stored.
 */
public final class RotationPlan {

    @NonNull private final List<SwapProposal> proposals;
    @NonNull private final List<PlanNote> notes;

    /** Copies both lists, so later changes to the caller's lists don't change the plan. */
    public RotationPlan(@NonNull List<SwapProposal> proposals, @NonNull List<PlanNote> notes) {
        this.proposals = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(proposals, "proposals")));
        this.notes = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(notes, "notes")));
    }

    /** The proposed swaps, in the order the swap rules chose. */
    @NonNull
    public List<SwapProposal> getProposals() {
        return proposals;
    }

    /** The notes, in the order the swap rules chose. */
    @NonNull
    public List<PlanNote> getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RotationPlan)) {
            return false;
        }
        RotationPlan that = (RotationPlan) other;
        return proposals.equals(that.proposals) && notes.equals(that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proposals, notes);
    }

    @NonNull
    @Override
    public String toString() {
        return "RotationPlan{proposals=" + proposals + ", notes=" + notes + "}";
    }
}

package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * The swap rules: a swap plan for the batches of one household (RotationPlannerContract;
 * SPEC.md, "RotationPlanner requirements").
 *
 * <p>Claude Code wrote this class on 2026-10-18 at the student's request (CLAUDE.md rules 1 and
 * 2), after the student approved its tests (RotationPlannerTest) and the decisions RP1 to RP9 in
 * SPEC.md. Claude Code changed it on 2026-10-22 at the student's request (swaps for every
 * category but medicine, documents, and subscriptions, with no food class or condition checks),
 * after the student approved the changed tests. See AI-USE.md.
 *
 * <p>A kit batch near its date can be swapped with a pantry batch of the same product whose date
 * is later and at least the replacement days away. A plan is only a proposal: it changes nothing
 * that is stored, and it says nothing about the items themselves. The kit batch goes to the
 * pantry for the household to review.
 */
public final class RotationPlanner implements RotationPlannerContract {

    /**
     * RP5 (the student's choice on 2026-10-22): the categories that can be swapped. Medicine,
     * documents, and subscriptions are left out. It lists what is allowed, so a category added to
     * ItemCategory later stays out until it is named here. The rules keep their own list: they
     * never call RuleRunner (SPEC.md, "The contracts").
     */
    private static final Set<ItemCategory> SWAPPED_CATEGORIES = EnumSet.of(ItemCategory.FOOD,
            ItemCategory.WATER, ItemCategory.FIRST_AID_SUPPLY, ItemCategory.COSMETIC,
            ItemCategory.OTHER);

    /**
     * RP6: a kit batch with one of these reasons still needs all of its units replaced. Because
     * of the order of the checks (RP5), this only reaches a kit batch of a swapped category whose
     * usable date is today or earlier.
     */
    private static final Set<PlanReason> NEEDS_ALL_OF_IT = EnumSet.of(PlanReason.DATE_TODAY_OR_PASSED);

    /** RP7: the soonest date first, then the lower batch id. */
    private static final Comparator<PlanBatch> SOONEST_DATE_THEN_ID =
            Comparator.comparing(RotationPlanner::dateOf).thenComparingLong(PlanBatch::getBatchId);

    /** Holds no data that changes, so the screens and the swap save can share one object. */
    public RotationPlanner() {
    }

    @NonNull
    @Override
    public RotationPlan plan(@NonNull List<PlanBatch> batches, @NonNull LocalDate today,
                             int nearDateDays, int replacementDays) {
        Objects.requireNonNull(batches, "batches");
        Objects.requireNonNull(today, "today");
        // RP9: Settings allow only 1 to 365 and 1 to 3650, so a negative number is a mistake in
        // the calling code. 0 works, and so does a pair that Settings would refuse.
        if (nearDateDays < 0) {
            throw new IllegalArgumentException("nearDateDays must not be negative: " + nearDateDays);
        }
        if (replacementDays < 0) {
            throw new IllegalArgumentException(
                    "replacementDays must not be negative: " + replacementDays);
        }

        // 1. Sort the batches into the planner's own lists; the given list is only read.
        List<PlanBatch> sources = new ArrayList<>();
        List<PlanBatch> pantryBatches = new ArrayList<>();
        // One note for each kit batch that has units, kept in batch id order (RP1, RP7).
        Map<Long, PlanNote> notes = new TreeMap<>();
        for (PlanBatch batch : batches) {
            if (batch.getQuantity() < 1) {
                continue; // RP2: nothing to swap and nothing to replace
            }
            if (isKit(batch.getLocationType())) {
                PlanReason reason = whyNotASource(batch, today, nearDateDays);
                if (reason == null) {
                    sources.add(batch);
                } else {
                    int stillNeeded = NEEDS_ALL_OF_IT.contains(reason) ? batch.getQuantity() : 0;
                    notes.put(batch.getBatchId(),
                            new PlanNote(batch.getBatchId(), reason, stillNeeded));
                }
            } else if (batch.getLocationType() == LocationType.PANTRY && canBeSwapped(batch)) {
                pantryBatches.add(batch);
            }
            // RP1: batches in Other locations are never part of a plan.
        }

        // 2. The soonest kit date chooses first, and each one takes the qualifying pantry
        //    batches with the earliest dates first (RP7).
        sources.sort(SOONEST_DATE_THEN_ID);
        pantryBatches.sort(SOONEST_DATE_THEN_ID);
        // How many units of each pantry batch no proposal has taken yet, so that each pantry
        // unit goes to one proposal at most.
        Map<Long, Integer> unitsLeft = new HashMap<>();
        for (PlanBatch pantry : pantryBatches) {
            unitsLeft.put(pantry.getBatchId(), pantry.getQuantity());
        }

        // 3. Propose swaps. A partial swap is allowed, and the same number of units moves each way.
        List<SwapProposal> proposals = new ArrayList<>();
        for (PlanBatch kit : sources) {
            int stillNeeded = kit.getQuantity();
            for (PlanBatch pantry : pantryBatches) {
                if (stillNeeded == 0) {
                    break;
                }
                int left = unitsLeft.get(pantry.getBatchId());
                if (left > 0 && replaces(pantry, kit, today, replacementDays)) {
                    int moving = Math.min(stillNeeded, left);
                    proposals.add(new SwapProposal(kit.getBatchId(), pantry.getBatchId(), moving));
                    unitsLeft.put(pantry.getBatchId(), left - moving);
                    stillNeeded -= moving;
                }
            }
            notes.put(kit.getBatchId(), new PlanNote(kit.getBatchId(),
                    swapReason(stillNeeded, kit.getQuantity()), stillNeeded));
        }
        return new RotationPlan(proposals, new ArrayList<>(notes.values()));
    }

    /**
     * Why a kit batch can't be a source, or null when a swap can be proposed for it (RP5).
     * The checks run in this order, and the first one that fails gives the reason.
     */
    @Nullable
    private static PlanReason whyNotASource(@NonNull PlanBatch kit, @NonNull LocalDate today,
                                            int nearDateDays) {
        if (!SWAPPED_CATEGORIES.contains(kit.getCategory())) {
            return PlanReason.CATEGORY_NOT_SWAPPED;
        }
        PlanReason dateReason = whyDateCantBeUsed(kit.getDate());
        if (dateReason != null) {
            return dateReason;
        }
        // Whole calendar days, like DatePolicy (DP4). The phone's clock is never read.
        long daysAway = ChronoUnit.DAYS.between(today, dateOf(kit));
        if (daysAway <= 0) {
            // The guide: never recommend pantry use for a source dated today or earlier.
            return PlanReason.DATE_TODAY_OR_PASSED;
        }
        if (daysAway > nearDateDays) {
            return PlanReason.NOT_NEAR_DATE;
        }
        // Since 2026-10-22 the food class and the condition answers don't matter (the student's
        // choice): every batch of a swapped category with a usable, near date is a source.
        return null;
    }

    /**
     * True when a pantry batch passes the checks that both batches of a swap need: a category
     * that can be swapped, and a date the rules can use (RP8).
     */
    private static boolean canBeSwapped(@NonNull PlanBatch batch) {
        return SWAPPED_CATEGORIES.contains(batch.getCategory())
                && whyDateCantBeUsed(batch.getDate()) == null;
    }

    /**
     * True when the pantry batch can replace the kit batch (RP8): the same product and unit,
     * a date later than the kit batch's, and at least {@code replacementDays} after today.
     * The replacement days are a planning choice, not a safety guarantee.
     */
    private static boolean replaces(@NonNull PlanBatch pantry, @NonNull PlanBatch kit,
                                    @NonNull LocalDate today, int replacementDays) {
        LocalDate pantryDate = dateOf(pantry);
        return pantry.getProductId() == kit.getProductId()
                && pantry.getUnit().equals(kit.getUnit())
                && pantryDate.isAfter(dateOf(kit))
                && ChronoUnit.DAYS.between(today, pantryDate) >= replacementDays;
    }

    /** Why the swap rules can't use a date (RP4), or null when they can. */
    @Nullable
    private static PlanReason whyDateCantBeUsed(@NonNull DateInput date) {
        if (date.getDate() == null) {
            // No date at all, or only part of one (printed text such as "03/2027").
            return PlanReason.DATE_MISSING_OR_INCOMPLETE;
        }
        if (!date.isConfirmed()) {
            return PlanReason.DATE_NOT_CONFIRMED;
        }
        if (!isKnownLabel(date.getLabel())) {
            return PlanReason.LABEL_TYPE_NOT_KNOWN;
        }
        return null;
    }

    /** RP3: Best by, Use by, and Expires. "Other" can be, for example, a packed-on date. */
    private static boolean isKnownLabel(@NonNull DateLabel label) {
        return label == DateLabel.BEST_BY || label == DateLabel.USE_BY
                || label == DateLabel.EXPIRES;
    }

    /** RP1: an Emergency kit or First aid location. The rules make this check themselves. */
    private static boolean isKit(@NonNull LocationType type) {
        return type == LocationType.EMERGENCY_KIT || type == LocationType.FIRST_AID;
    }

    /** RP6: the reason for a source, from how many of its units no swap covers. */
    @NonNull
    private static PlanReason swapReason(int stillNeeded, int quantity) {
        if (stillNeeded == 0) {
            return PlanReason.SWAP_PROPOSED;
        } else if (stillNeeded == quantity) {
            return PlanReason.NO_REPLACEMENT;
        }
        return PlanReason.PARTLY_REPLACED;
    }

    /** The complete date of a batch whose date the rules can use (RP4). */
    @NonNull
    private static LocalDate dateOf(@NonNull PlanBatch batch) {
        return Objects.requireNonNull(batch.getDate().getDate(),
                "Only a batch with a complete date is compared by its date");
    }
}

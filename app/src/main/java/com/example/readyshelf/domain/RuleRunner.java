package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Runs the student's rules over the batches and prepares the lists that the screens show.
 *
 * <p>This class makes no date or swap decisions of its own. It calls the two contracts,
 * sorts their answers, and applies the app's safety checks for swaps and replacement needs
 * (see SPEC.md). If the rules throw an exception, it reaches the caller, and the screen shows
 * it as text.
 */
public final class RuleRunner {

    private static final Comparator<BatchWithStatus> SOONEST_DATE_FIRST = Comparator.comparing(
            BatchWithStatus::getBatch, InventoryFilter.byDate(InventoryFilter.DateOrder.SOONEST_FIRST));

    private static final Comparator<ShownNeed> NEED_SOONEST_DATE_FIRST = Comparator.comparing(
            ShownNeed::getBatch, InventoryFilter.byDate(InventoryFilter.DateOrder.SOONEST_FIRST));

    // The categories a household buys again (the student's choice on 2026-10-22). Only they are
    // swapped, and only they get replacement needs: kit batches that the swaps don't cover, and
    // expired batches in any place. Medicine is left out because DueWise gives no advice about
    // replacing medicine; documents and subscriptions are renewed, not bought (since 2026-10-24
    // they are under Renewal instead; see RenewalList). The swap rules keep a list of their own
    // (RotationPlanner); this one is the app's safety net.
    private static final Set<ItemCategory> BOUGHT_AGAIN = EnumSet.of(ItemCategory.FOOD,
            ItemCategory.WATER, ItemCategory.FIRST_AID_SUPPLY, ItemCategory.COSMETIC,
            ItemCategory.OTHER);

    /** One proposed swap that passed the app's safety checks, with both batches. Never changes. */
    public static final class ShownSwap {
        @NonNull private final BatchInfo kitBatch;
        @NonNull private final BatchInfo pantryBatch;
        private final int quantity;

        public ShownSwap(@NonNull BatchInfo kitBatch, @NonNull BatchInfo pantryBatch, int quantity) {
            this.kitBatch = Objects.requireNonNull(kitBatch, "kitBatch");
            this.pantryBatch = Objects.requireNonNull(pantryBatch, "pantryBatch");
            this.quantity = quantity;
        }

        @NonNull
        public BatchInfo getKitBatch() {
            return kitBatch;
        }

        @NonNull
        public BatchInfo getPantryBatch() {
            return pantryBatch;
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
            if (!(other instanceof ShownSwap)) {
                return false;
            }
            ShownSwap that = (ShownSwap) other;
            return quantity == that.quantity && kitBatch.equals(that.kitBatch)
                    && pantryBatch.equals(that.pantryBatch);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kitBatch, pantryBatch, quantity);
        }

        @NonNull
        @Override
        public String toString() {
            return "ShownSwap{kit=" + kitBatch.getId() + ", pantry=" + pantryBatch.getId()
                    + ", quantity=" + quantity + "}";
        }
    }

    /** A batch that still needs replacement, with the plan's reason. Never changes. */
    public static final class ShownNeed {
        @NonNull private final BatchInfo batch;
        @NonNull private final PlanReason reason;
        private final int stillNeeded;

        public ShownNeed(@NonNull BatchInfo batch, @NonNull PlanReason reason, int stillNeeded) {
            this.batch = Objects.requireNonNull(batch, "batch");
            this.reason = Objects.requireNonNull(reason, "reason");
            this.stillNeeded = stillNeeded;
        }

        @NonNull
        public BatchInfo getBatch() {
            return batch;
        }

        @NonNull
        public PlanReason getReason() {
            return reason;
        }

        public int getStillNeeded() {
            return stillNeeded;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ShownNeed)) {
                return false;
            }
            ShownNeed that = (ShownNeed) other;
            return stillNeeded == that.stillNeeded && batch.equals(that.batch) && reason == that.reason;
        }

        @Override
        public int hashCode() {
            return Objects.hash(batch, reason, stillNeeded);
        }

        @NonNull
        @Override
        public String toString() {
            return "ShownNeed{batch=" + batch.getId() + ", " + reason + ", stillNeeded=" + stillNeeded + "}";
        }
    }

    /** The parts of a swap plan that the screens show. Never changes. */
    public static final class ShownPlan {
        @NonNull private final List<ShownSwap> swaps;
        @NonNull private final List<ShownNeed> needs;

        ShownPlan(@NonNull List<ShownSwap> swaps, @NonNull List<ShownNeed> needs) {
            this.swaps = Collections.unmodifiableList(new ArrayList<>(swaps));
            this.needs = Collections.unmodifiableList(new ArrayList<>(needs));
        }

        /** The swaps for Home, in the order the swap rules chose. */
        @NonNull
        public List<ShownSwap> getSwaps() {
            return swaps;
        }

        /**
         * What still needs replacement, for the Purchase list: kit batches of the categories a
         * household buys again that the swaps don't cover, and expired batches of those
         * categories in any place. At most one need for each batch, soonest date first.
         */
        @NonNull
        public List<ShownNeed> getNeeds() {
            return needs;
        }
    }

    private RuleRunner() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Every batch with its status, in the same order as {@code batches}. The screens and the
     * daily reminder check (on a background thread) share one policy object, so only one of
     * them asks it at a time.
     */
    @NonNull
    public static List<BatchWithStatus> withStatuses(@NonNull List<BatchInfo> batches,
                                                     @NonNull DatePolicyContract policy,
                                                     @NonNull LocalDate today,
                                                     @NonNull DateThresholds thresholds) {
        List<BatchWithStatus> result = new ArrayList<>();
        synchronized (policy) {
            for (BatchInfo batch : batches) {
                result.add(new BatchWithStatus(batch, statusOf(batch, policy, today, thresholds)));
            }
        }
        return result;
    }

    /**
     * The status of each batch that is not archived, by batch id. Archived batches are never
     * given to the date rules, so they get no entry. For screens that can also list archived
     * batches (Inventory with "Show archived batches", Batch details).
     */
    @NonNull
    public static Map<Long, DateStatus> statusesOfActive(@NonNull List<BatchInfo> batches,
                                                        @NonNull DatePolicyContract policy,
                                                        @NonNull LocalDate today,
                                                        @NonNull DateThresholds thresholds) {
        List<BatchInfo> active = new ArrayList<>();
        for (BatchInfo batch : batches) {
            if (!batch.isArchived()) {
                active.add(batch);
            }
        }
        Map<Long, DateStatus> statuses = new HashMap<>();
        for (BatchWithStatus item : withStatuses(active, policy, today, thresholds)) {
            statuses.put(item.getBatch().getId(), item.getStatus());
        }
        return statuses;
    }

    /** The batches whose status is anything but LATER, soonest date first. */
    @NonNull
    public static List<BatchWithStatus> needsReview(@NonNull List<BatchInfo> batches,
                                                    @NonNull DatePolicyContract policy,
                                                    @NonNull LocalDate today,
                                                    @NonNull DateThresholds thresholds) {
        List<BatchWithStatus> result = new ArrayList<>();
        for (BatchWithStatus item : withStatuses(batches, policy, today, thresholds)) {
            if (item.getStatus() != DateStatus.LATER) {
                result.add(item);
            }
        }
        result.sort(SOONEST_DATE_FIRST);
        return result;
    }

    /**
     * Asks the swap rules for a plan, then keeps the parts that pass the app's safety checks.
     *
     * <p>The swap rules get every batch in {@code batches}, in a list that can't be changed,
     * with each product's food class (the swap rules don't use it since 2026-10-22). The screens
     * and the swap save (on a background thread) share one planner object, so only one plan is
     * made at a time.
     */
    @NonNull
    public static ShownPlan plan(@NonNull List<BatchInfo> batches,
                                 @NonNull RotationPlannerContract planner,
                                 @NonNull LocalDate today,
                                 @NonNull DateThresholds thresholds) {
        Map<Long, BatchInfo> byId = new HashMap<>();
        List<PlanBatch> input = new ArrayList<>();
        for (BatchInfo batch : batches) {
            byId.put(batch.getId(), batch);
            input.add(PlanBatch.from(batch, batch.getFoodClass()));
        }

        RotationPlan plan;
        synchronized (planner) {
            plan = planner.plan(Collections.unmodifiableList(input), today,
                    thresholds.getNearDateDays(), thresholds.getReplacementDays());
        }
        if (plan == null) {
            throw new IllegalStateException(
                    "RotationPlanner.plan returned null. Return a RotationPlan with empty lists instead.");
        }

        List<ShownSwap> swaps = new ArrayList<>();
        // How many units of each batch the swaps kept so far already move.
        Map<Long, Integer> unitsMoving = new HashMap<>();
        for (SwapProposal proposal : plan.getProposals()) {
            if (proposal != null && passesSafetyChecks(proposal, byId, today, unitsMoving)) {
                BatchInfo kit = byId.get(proposal.getKitBatchId());
                BatchInfo pantry = byId.get(proposal.getPantryBatchId());
                int quantity = proposal.getQuantity();
                unitsMoving.put(kit.getId(), unitsMoving(unitsMoving, kit) + quantity);
                unitsMoving.put(pantry.getId(), unitsMoving(unitsMoving, pantry) + quantity);
                swaps.add(new ShownSwap(kit, pantry, quantity));
            }
        }
        return new ShownPlan(swaps, needs(plan.getNotes(), batches, byId, today));
    }

    /**
     * The app's safety net for one proposal. It is not one of the student's rules, and the
     * student's tests must still prove those rules. These checks keep a swap from moving stock
     * that doesn't exist or doesn't belong in a swap. A proposal is shown only when:
     * <ul>
     *   <li>both batches were given to the swap rules, and they are two different batches;</li>
     *   <li>both batches are of a category the household buys again (food, water, first aid
     *       supplies, cosmetics, or Other), so medicine, documents, and subscriptions are never
     *       swapped;</li>
     *   <li>the kit batch is in a kit (Emergency kit or First aid location), and the pantry
     *       batch is in a Pantry location;</li>
     *   <li>both batches are the same product, so they have the same unit;</li>
     *   <li>both batches have a complete date after today, so a batch dated today or earlier is
     *       never sent to the pantry, and is never put into a kit;</li>
     *   <li>made-up sample data is not paired with the household's real batches;</li>
     *   <li>the quantity is at least 1, and, together with the swaps already kept, no batch
     *       gives more units than it has.</li>
     * </ul>
     * Which pantry batch is a good replacement (its date and the replacement days) is decided
     * only by the swap rules.
     */
    private static boolean passesSafetyChecks(@NonNull SwapProposal proposal,
                                              @NonNull Map<Long, BatchInfo> byId,
                                              @NonNull LocalDate today,
                                              @NonNull Map<Long, Integer> unitsMoving) {
        BatchInfo kit = byId.get(proposal.getKitBatchId());
        BatchInfo pantry = byId.get(proposal.getPantryBatchId());
        if (kit == null || pantry == null || kit.getId() == pantry.getId()) {
            return false;
        }
        int quantity = proposal.getQuantity();
        boolean bothBoughtAgain = BOUGHT_AGAIN.contains(kit.getCategory())
                && BOUGHT_AGAIN.contains(pantry.getCategory());
        boolean rightPlaces = StockChange.isKit(kit.getLocationType())
                && pantry.getLocationType() == LocationType.PANTRY;
        boolean sameProduct = kit.getProductId() == pantry.getProductId();
        boolean bothDatesAfterToday = isAfter(kit.getDate(), today) && isAfter(pantry.getDate(), today);
        boolean sameKindOfData = kit.isSample() == pantry.isSample();
        boolean unitsAvailable = quantity >= 1
                && quantity <= kit.getQuantity() - unitsMoving(unitsMoving, kit)
                && quantity <= pantry.getQuantity() - unitsMoving(unitsMoving, pantry);
        return bothBoughtAgain && rightPlaces && sameProduct && bothDatesAfterToday
                && sameKindOfData && unitsAvailable;
    }

    /**
     * The needs that the Purchase list shows, from the plan's notes and one app check.
     * <ul>
     *   <li>The notes count only for kit batches of a category the household buys again: what
     *       the swaps don't cover. Notes about medicine, documents, subscriptions, and batches
     *       outside kits are left out. Until 2026-10-22 only kit food counted.</li>
     *   <li>A need is never more than the batch's quantity, and a batch gets at most one need
     *       (the largest one its notes give).</li>
     *   <li>Expired batches (not one of the student's rules): a batch of a category the household
     *       buys again (food, water, first aid supplies, cosmetics, and other), in any place, with
     *       a complete date of today or earlier always needs replacement for its whole quantity.
     *       When the notes give it less, the need is its whole quantity with the reason
     *       DATE_TODAY_OR_PASSED. Such a batch is never part of a swap (see the swap checks
     *       above). Until 2026-10-22 only kit food got this need.</li>
     *   <li>Medicine, documents, and subscriptions never get a need of their own.</li>
     * </ul>
     */
    @NonNull
    private static List<ShownNeed> needs(@NonNull List<PlanNote> notes,
                                         @NonNull List<BatchInfo> batches,
                                         @NonNull Map<Long, BatchInfo> byId,
                                         @NonNull LocalDate today) {
        // Keyed by batch id, in the notes' order (sorted at the end).
        Map<Long, ShownNeed> byBatch = new LinkedHashMap<>();
        for (PlanNote note : notes) {
            BatchInfo batch = note == null ? null : byId.get(note.getBatchId());
            if (batch == null || !isKitBatchBoughtAgain(batch)) {
                continue;
            }
            int amount = Math.min(note.getStillNeeded(), batch.getQuantity());
            ShownNeed earlier = byBatch.get(batch.getId());
            if (amount > 0 && (earlier == null || amount > earlier.getStillNeeded())) {
                byBatch.put(batch.getId(), new ShownNeed(batch, note.getReason(), amount));
            }
        }
        for (BatchInfo batch : batches) {
            boolean datedTodayOrEarlier = batch.getDate() != null && !batch.getDate().isAfter(today);
            if (BOUGHT_AGAIN.contains(batch.getCategory()) && datedTodayOrEarlier
                    && batch.getQuantity() > 0) {
                ShownNeed earlier = byBatch.get(batch.getId());
                if (earlier == null || earlier.getStillNeeded() < batch.getQuantity()) {
                    byBatch.put(batch.getId(), new ShownNeed(batch, PlanReason.DATE_TODAY_OR_PASSED,
                            batch.getQuantity()));
                }
            }
        }
        List<ShownNeed> result = new ArrayList<>(byBatch.values());
        result.sort(NEED_SOONEST_DATE_FIRST);
        return result;
    }

    /**
     * True for a batch of a category the household buys again, in an Emergency kit or First aid
     * location.
     */
    private static boolean isKitBatchBoughtAgain(@NonNull BatchInfo batch) {
        return BOUGHT_AGAIN.contains(batch.getCategory()) && StockChange.isKit(batch.getLocationType());
    }

    private static boolean isAfter(@Nullable LocalDate date, @NonNull LocalDate today) {
        return date != null && date.isAfter(today);
    }

    private static int unitsMoving(@NonNull Map<Long, Integer> unitsMoving, @NonNull BatchInfo batch) {
        Integer units = unitsMoving.get(batch.getId());
        return units == null ? 0 : units;
    }

    private static DateStatus statusOf(BatchInfo batch, DatePolicyContract policy,
                                       LocalDate today, DateThresholds thresholds) {
        DateStatus status = policy.statusOf(DateInput.from(batch), today, thresholds.getNearDateDays());
        if (status == null) {
            throw new IllegalStateException("DatePolicy.statusOf returned null for batch "
                    + batch.getId() + " (" + batch.getName() + "). It must return a DateStatus.");
        }
        return status;
    }
}

package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The amounts before and after one change to a batch's stock, for the review screens.
 * An object of this class never changes. This is plain Java, so it can be tested without a phone.
 *
 * <p>Kit amounts: when stock of a product leaves an emergency kit or a first aid location,
 * the review shows that product's total in that location before and after. ReadyShelf doesn't
 * know how much a kit should hold, so it never says that a kit is complete.
 */
public final class StockChange {

    /** The largest quantity one batch can have. */
    public static final int MAX_QUANTITY = 9999;

    /** An amount before and after the change. */
    public static final class Amounts {
        private final int before;
        private final int after;

        public Amounts(int before, int after) {
            this.before = before;
            this.after = after;
        }

        public int getBefore() {
            return before;
        }

        public int getAfter() {
            return after;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Amounts)) {
                return false;
            }
            Amounts that = (Amounts) other;
            return before == that.before && after == that.after;
        }

        @Override
        public int hashCode() {
            return Objects.hash(before, after);
        }

        @NonNull
        @Override
        public String toString() {
            return before + " -> " + after;
        }
    }

    @NonNull private final Amounts batch;
    @Nullable private final Amounts kit;
    @Nullable private final Amounts destination;

    private StockChange(@NonNull Amounts batch, @Nullable Amounts kit, @Nullable Amounts destination) {
        this.batch = batch;
        this.kit = kit;
        this.destination = destination;
    }

    /**
     * Used or discarded: {@code amount} leaves the batch.
     *
     * @param activeBatches the batches that are not archived (this batch may be among them)
     */
    @NonNull
    public static StockChange removal(@NonNull BatchInfo batch, int amount,
                                      @NonNull List<BatchInfo> activeBatches) {
        checkAmount(batch, amount);
        int left = batch.getQuantity() - amount;
        return new StockChange(new Amounts(batch.getQuantity(), left),
                kitAmounts(batch, left, activeBatches), null);
    }

    /**
     * Move: {@code amount} goes from the batch's location to another location.
     * The batch amounts are what stays at the old location. The destination amounts are
     * the product's total at the new location.
     */
    @NonNull
    public static StockChange move(@NonNull BatchInfo batch, int amount, long destinationId,
                                   @NonNull List<BatchInfo> activeBatches) {
        checkAmount(batch, amount);
        if (destinationId == batch.getLocationId()) {
            throw new IllegalArgumentException("A move needs a different location");
        }
        int left = batch.getQuantity() - amount;
        int destinationBefore = otherAmountAt(activeBatches, batch.getProductId(), destinationId,
                batch.getId());
        return new StockChange(new Amounts(batch.getQuantity(), left),
                kitAmounts(batch, left, activeBatches),
                new Amounts(destinationBefore, destinationBefore + amount));
    }

    /** Archive: the batch keeps its quantity, but it leaves the active stock. */
    @NonNull
    public static StockChange archive(@NonNull BatchInfo batch,
                                      @NonNull List<BatchInfo> activeBatches) {
        return new StockChange(new Amounts(batch.getQuantity(), batch.getQuantity()),
                kitAmounts(batch, 0, activeBatches), null);
    }

    /**
     * A count corrected on the Edit form. When the batch also gets a different product,
     * none of it counts for the old product any more.
     *
     * @param newProductId the product chosen on the form, or 0 for a new product
     */
    @NonNull
    public static StockChange countCorrection(@NonNull BatchInfo batch, long newProductId,
                                              int newQuantity,
                                              @NonNull List<BatchInfo> activeBatches) {
        int stillThisProduct = newProductId == batch.getProductId() ? newQuantity : 0;
        return new StockChange(new Amounts(batch.getQuantity(), newQuantity),
                kitAmounts(batch, stillThisProduct, activeBatches), null);
    }

    /** The batch's quantity before and after. */
    @NonNull
    public Amounts getBatch() {
        return batch;
    }

    /**
     * The product's total in the batch's kit before and after, or null when the batch is not
     * in a kit or the kit's total doesn't go down.
     */
    @Nullable
    public Amounts getKit() {
        return kit;
    }

    /** For a move: the product's total at the new location before and after. Otherwise null. */
    @Nullable
    public Amounts getDestination() {
        return destination;
    }

    /** True for the location types that count as kits: Emergency kit and First aid. */
    public static boolean isKit(@NonNull LocationType type) {
        return type == LocationType.EMERGENCY_KIT || type == LocationType.FIRST_AID;
    }

    /**
     * The total quantity of one product at one location, leaving out one batch and every
     * archived batch.
     */
    public static int otherAmountAt(@NonNull List<BatchInfo> batches, long productId,
                                    long locationId, long leaveOutBatchId) {
        int total = 0;
        for (BatchInfo other : batches) {
            if (other.getId() != leaveOutBatchId && !other.isArchived()
                    && other.getProductId() == productId && other.getLocationId() == locationId) {
                total += other.getQuantity();
            }
        }
        return total;
    }

    /**
     * Reads a quantity typed on the batch form: a whole number from 0 to {@link #MAX_QUANTITY}.
     * Returns null for anything else.
     */
    @Nullable
    public static Integer parseQuantity(@Nullable String text) {
        return parseWholeNumber(text, 0, MAX_QUANTITY);
    }

    /**
     * Reads the amount for "Some": a whole number from 1 to {@code available}.
     * Returns null for anything else.
     */
    @Nullable
    public static Integer parsePartAmount(@Nullable String text, int available) {
        return parseWholeNumber(text, 1, available);
    }

    @Nullable
    private static Integer parseWholeNumber(@Nullable String text, int smallest, int largest) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        // Only digits: no plus or minus sign, no spaces, no decimal point.
        if (trimmed.isEmpty() || trimmed.length() > 4 || !trimmed.chars().allMatch(Character::isDigit)) {
            return null;
        }
        int value = Integer.parseInt(trimmed);
        return value >= smallest && value <= largest ? value : null;
    }

    private static void checkAmount(@NonNull BatchInfo batch, int amount) {
        if (amount < 1 || amount > batch.getQuantity()) {
            throw new IllegalArgumentException("The amount must be from 1 to "
                    + batch.getQuantity() + ", not " + amount);
        }
    }

    /** The kit total before and after, when the batch is in a kit and the total goes down. */
    @Nullable
    private static Amounts kitAmounts(@NonNull BatchInfo batch, int leftInThisBatch,
                                      @NonNull List<BatchInfo> activeBatches) {
        if (!isKit(batch.getLocationType()) || batch.isArchived()) {
            return null;
        }
        int others = otherAmountAt(activeBatches, batch.getProductId(), batch.getLocationId(),
                batch.getId());
        int before = others + batch.getQuantity();
        int after = others + leftInThisBatch;
        return after < before ? new Amounts(before, after) : null;
    }
}

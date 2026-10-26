package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * The amounts in each place before and after one swap, for the Review swap screen.
 * An object of this class never changes. This is plain Java, so it can be tested without a phone.
 *
 * <p>It only counts units. It never compares dates: which batches qualify is decided by the
 * swap rules (the student's RotationPlanner).
 */
public final class SwapPreview {

    private final int quantity;
    private final int kitBatchLeft;
    private final int pantryBatchLeft;
    @NonNull private final StockChange.Amounts kitTotal;
    @NonNull private final StockChange.Amounts pantryTotal;

    private SwapPreview(int quantity, int kitBatchLeft, int pantryBatchLeft,
                        @NonNull StockChange.Amounts kitTotal,
                        @NonNull StockChange.Amounts pantryTotal) {
        this.quantity = quantity;
        this.kitBatchLeft = kitBatchLeft;
        this.pantryBatchLeft = pantryBatchLeft;
        this.kitTotal = kitTotal;
        this.pantryTotal = pantryTotal;
    }

    /**
     * The amounts for one swap.
     *
     * @param activeBatches the batches that are not archived (the two swap batches among them),
     *                      for the product's totals in each place
     * @throws IllegalArgumentException when the two batches are different products, or the
     *                                  quantity is not from 1 to each batch's quantity
     */
    @NonNull
    public static SwapPreview of(@NonNull RuleRunner.ShownSwap swap,
                                 @NonNull List<BatchInfo> activeBatches) {
        BatchInfo kit = swap.getKitBatch();
        BatchInfo pantry = swap.getPantryBatch();
        int quantity = swap.getQuantity();
        if (kit.getProductId() != pantry.getProductId()) {
            throw new IllegalArgumentException("A swap needs two batches of the same product");
        }
        if (quantity < 1 || quantity > kit.getQuantity() || quantity > pantry.getQuantity()) {
            throw new IllegalArgumentException("The amount must be from 1 to "
                    + Math.min(kit.getQuantity(), pantry.getQuantity()) + ", not " + quantity);
        }
        int kitOthers = StockChange.otherAmountAt(activeBatches, kit.getProductId(),
                kit.getLocationId(), kit.getId());
        int pantryOthers = StockChange.otherAmountAt(activeBatches, pantry.getProductId(),
                pantry.getLocationId(), pantry.getId());
        int kitLeft = kit.getQuantity() - quantity;
        int pantryLeft = pantry.getQuantity() - quantity;
        // The same amount of the same product goes each way, so each place keeps its total.
        StockChange.Amounts kitTotal = new StockChange.Amounts(kitOthers + kit.getQuantity(),
                kitOthers + kitLeft + quantity);
        StockChange.Amounts pantryTotal = new StockChange.Amounts(
                pantryOthers + pantry.getQuantity(), pantryOthers + pantryLeft + quantity);
        return new SwapPreview(quantity, kitLeft, pantryLeft, kitTotal, pantryTotal);
    }

    /** How many units move each way. */
    public int getQuantity() {
        return quantity;
    }

    /** What stays in the kit from the kit batch (0 when all of it moves). */
    public int getKitBatchLeft() {
        return kitBatchLeft;
    }

    /** What stays in the pantry from the pantry batch (0 when all of it moves). */
    public int getPantryBatchLeft() {
        return pantryBatchLeft;
    }

    /** True when only part of the kit batch moves, so the moving part becomes its own batch. */
    public boolean isKitBatchSplit() {
        return kitBatchLeft > 0;
    }

    /** True when only part of the pantry batch moves, so the moving part becomes its own batch. */
    public boolean isPantryBatchSplit() {
        return pantryBatchLeft > 0;
    }

    /** The product's total in the kit before and after. */
    @NonNull
    public StockChange.Amounts getKitTotal() {
        return kitTotal;
    }

    /** The product's total in the pantry before and after. */
    @NonNull
    public StockChange.Amounts getPantryTotal() {
        return pantryTotal;
    }
}

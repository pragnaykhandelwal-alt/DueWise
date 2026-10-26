package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for SwapPreview: the amounts in each place before and after a swap.
 * The swaps here are made up by each test; no swap rules are involved.
 */
public class SwapPreviewTest {

    private static final long BEANS = 3;
    private static final long KIT = 2;
    private static final long PANTRY = 1;

    private static final BatchInfo KIT_BEANS = batch(10, BEANS, 5, LocalDate.of(2026, 10, 20),
            KIT, LocationType.EMERGENCY_KIT);
    private static final BatchInfo PANTRY_BEANS = batch(11, BEANS, 10, LocalDate.of(2027, 6, 1),
            PANTRY, LocationType.PANTRY);

    @Test
    public void partOfEachBatch_leavesTheRestInPlace_andKeepsEachPlacesTotal() {
        SwapPreview preview = SwapPreview.of(new RuleRunner.ShownSwap(KIT_BEANS, PANTRY_BEANS, 3),
                Arrays.asList(KIT_BEANS, PANTRY_BEANS));

        assertEquals(3, preview.getQuantity());
        assertEquals(2, preview.getKitBatchLeft());
        assertEquals(7, preview.getPantryBatchLeft());
        assertTrue(preview.isKitBatchSplit());
        assertTrue(preview.isPantryBatchSplit());
        assertEquals(new StockChange.Amounts(5, 5), preview.getKitTotal());
        assertEquals(new StockChange.Amounts(10, 10), preview.getPantryTotal());
    }

    @Test
    public void allOfTheKitBatch_leavesNothingOfItInTheKit() {
        SwapPreview preview = SwapPreview.of(new RuleRunner.ShownSwap(KIT_BEANS, PANTRY_BEANS, 5),
                Arrays.asList(KIT_BEANS, PANTRY_BEANS));

        assertEquals(0, preview.getKitBatchLeft());
        assertFalse(preview.isKitBatchSplit());
        assertEquals(5, preview.getPantryBatchLeft());
        assertTrue(preview.isPantryBatchSplit());
    }

    @Test
    public void allOfThePantryBatch_sixAndTwo_leavesFourInTheKitAndNothingOfItInThePantry() {
        BatchInfo kitSix = batch(14, BEANS, 6, LocalDate.of(2026, 10, 20), KIT,
                LocationType.EMERGENCY_KIT);
        BatchInfo pantryTwo = batch(15, BEANS, 2, LocalDate.of(2027, 6, 1), PANTRY,
                LocationType.PANTRY);

        SwapPreview preview = SwapPreview.of(new RuleRunner.ShownSwap(kitSix, pantryTwo, 2),
                Arrays.asList(kitSix, pantryTwo));

        assertEquals(2, preview.getQuantity());
        assertEquals(4, preview.getKitBatchLeft());
        assertTrue(preview.isKitBatchSplit());
        assertEquals(0, preview.getPantryBatchLeft());
        assertFalse(preview.isPantryBatchSplit());
        // Each place keeps its total: the kit 4 + 2, the pantry 0 + 2.
        assertEquals(new StockChange.Amounts(6, 6), preview.getKitTotal());
        assertEquals(new StockChange.Amounts(2, 2), preview.getPantryTotal());
    }

    @Test
    public void totals_countOtherActiveBatchesOfTheSameProductInEachPlace() {
        BatchInfo moreKitBeans = batch(12, BEANS, 4, LocalDate.of(2027, 1, 1), KIT,
                LocationType.EMERGENCY_KIT);
        BatchInfo pantryRice = batch(13, 9, 8, LocalDate.of(2027, 1, 1), PANTRY,
                LocationType.PANTRY);
        List<BatchInfo> active = Arrays.asList(KIT_BEANS, PANTRY_BEANS, moreKitBeans, pantryRice);

        SwapPreview preview = SwapPreview.of(new RuleRunner.ShownSwap(KIT_BEANS, PANTRY_BEANS, 3),
                active);

        assertEquals(new StockChange.Amounts(9, 9), preview.getKitTotal());     // 5 + 4 beans
        assertEquals(new StockChange.Amounts(10, 10), preview.getPantryTotal()); // rice not counted
    }

    @Test
    public void refusesDifferentProductsOrAnAmountThatDoesntFit() {
        BatchInfo pantryRice = batch(13, 9, 8, LocalDate.of(2027, 1, 1), PANTRY, LocationType.PANTRY);
        List<BatchInfo> active = Arrays.asList(KIT_BEANS, PANTRY_BEANS, pantryRice);

        assertThrows(IllegalArgumentException.class, () -> SwapPreview.of(
                new RuleRunner.ShownSwap(KIT_BEANS, pantryRice, 1), active));
        assertThrows(IllegalArgumentException.class, () -> SwapPreview.of(
                new RuleRunner.ShownSwap(KIT_BEANS, PANTRY_BEANS, 0), active));
        assertThrows(IllegalArgumentException.class, () -> SwapPreview.of(
                new RuleRunner.ShownSwap(KIT_BEANS, PANTRY_BEANS, 6), active)); // the kit has 5
    }

    private static BatchInfo batch(long id, long productId, int quantity, LocalDate date,
                                   long locationId, LocationType locationType) {
        return new BatchInfo(id, productId, "Product " + productId, "", "", "cans",
                FoodClass.SHELF_STABLE_FOOD, ItemCategory.FOOD, quantity, date, null,
                DateLabel.BEST_BY, true, locationId, locationType.name(), locationType, null,
                ConditionAnswer.YES, null, ConditionAnswer.NO, ConditionAnswer.NO, false, false);
    }
}

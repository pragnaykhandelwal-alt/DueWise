package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Tests for the amounts that the stock review screens show. */
public class StockChangeTest {

    private static final long KIT_ID = 2;
    private static final long PANTRY_ID = 1;
    private static final long FIRST_AID_ID = 3;
    private static final long BEANS = 10;
    private static final long RICE = 11;

    // Canned beans: 4 cans in the kit (this batch), 2 more in the kit, 5 in the pantry.
    private static final BatchInfo BEANS_KIT = batch(1, BEANS, 4, KIT_ID, false);
    private static final BatchInfo MORE_BEANS_KIT = batch(2, BEANS, 2, KIT_ID, false);
    private static final BatchInfo BEANS_PANTRY = batch(3, BEANS, 5, PANTRY_ID, false);
    private static final BatchInfo RICE_KIT = batch(4, RICE, 7, KIT_ID, false);
    private static final BatchInfo ARCHIVED_BEANS_KIT = batch(5, BEANS, 9, KIT_ID, true);

    private static final List<BatchInfo> ALL =
            Arrays.asList(BEANS_KIT, MORE_BEANS_KIT, BEANS_PANTRY, RICE_KIT, ARCHIVED_BEANS_KIT);

    @Test
    public void removalFromAKit_showsTheProductTotalInThatKitBeforeAndAfter() {
        StockChange change = StockChange.removal(BEANS_KIT, 3, ALL);

        assertEquals(new StockChange.Amounts(4, 1), change.getBatch());
        // 4 + 2 cans of beans in the kit; rice and the archived batch don't count.
        assertEquals(new StockChange.Amounts(6, 3), change.getKit());
        assertNull(change.getDestination());
    }

    @Test
    public void removalOfAllOfIt_leavesZero() {
        StockChange change = StockChange.removal(BEANS_KIT, 4, ALL);

        assertEquals(new StockChange.Amounts(4, 0), change.getBatch());
        assertEquals(new StockChange.Amounts(6, 2), change.getKit());
    }

    @Test
    public void removalFromAPantry_hasNoKitAmounts() {
        StockChange change = StockChange.removal(BEANS_PANTRY, 2, ALL);

        assertEquals(new StockChange.Amounts(5, 3), change.getBatch());
        assertNull(change.getKit());
    }

    @Test
    public void firstAidLocationsCountAsKits() {
        BatchInfo gauze = new BatchInfo(6, 12, "Gauze pads", "", "", "boxes",
                ItemCategory.FIRST_AID_SUPPLY, 2, null, null, DateLabel.EXPIRES, false,
                FIRST_AID_ID, "First aid box", LocationType.FIRST_AID, null, null, null, null,
                null, false, false);

        StockChange change = StockChange.removal(gauze, 1, Collections.singletonList(gauze));

        assertEquals(new StockChange.Amounts(2, 1), change.getKit());
        assertTrue(StockChange.isKit(LocationType.EMERGENCY_KIT));
        assertTrue(StockChange.isKit(LocationType.FIRST_AID));
    }

    @Test
    public void moveSomeOutOfAKit_showsKitAndDestinationTotals() {
        StockChange change = StockChange.move(BEANS_KIT, 3, PANTRY_ID, ALL);

        assertEquals(new StockChange.Amounts(4, 1), change.getBatch());
        assertEquals(new StockChange.Amounts(6, 3), change.getKit());
        assertEquals(new StockChange.Amounts(5, 8), change.getDestination());
    }

    @Test
    public void moveIntoAKit_hasNoKitWarningAmounts() {
        StockChange change = StockChange.move(BEANS_PANTRY, 5, KIT_ID, ALL);

        assertEquals(new StockChange.Amounts(5, 0), change.getBatch());
        assertNull(change.getKit());
        // 4 + 2 cans already in the kit; the archived batch doesn't count.
        assertEquals(new StockChange.Amounts(6, 11), change.getDestination());
    }

    @Test
    public void moveToTheSameLocation_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> StockChange.move(BEANS_KIT, 1, KIT_ID, ALL));
    }

    @Test
    public void amountsOutsideOneToTheQuantity_areRefused() {
        assertThrows(IllegalArgumentException.class, () -> StockChange.removal(BEANS_KIT, 0, ALL));
        assertThrows(IllegalArgumentException.class, () -> StockChange.removal(BEANS_KIT, 5, ALL));
    }

    @Test
    public void archiveInAKit_takesTheWholeBatchOutOfTheKitTotal() {
        StockChange change = StockChange.archive(BEANS_KIT, ALL);

        assertEquals(new StockChange.Amounts(4, 4), change.getBatch());
        assertEquals(new StockChange.Amounts(6, 2), change.getKit());
    }

    @Test
    public void archiveOfAnEmptyBatch_hasNoKitAmounts() {
        BatchInfo empty = batch(7, BEANS, 0, KIT_ID, false);

        assertNull(StockChange.archive(empty, ALL).getKit());
    }

    @Test
    public void countCorrection_lowerInAKit_showsKitAmounts_higherDoesNot() {
        assertEquals(new StockChange.Amounts(6, 5),
                StockChange.countCorrection(BEANS_KIT, BEANS, 3, ALL).getKit());
        assertNull(StockChange.countCorrection(BEANS_KIT, BEANS, 8, ALL).getKit());
    }

    @Test
    public void countCorrection_withAnotherProduct_takesAllOfItFromTheOldProduct() {
        StockChange change = StockChange.countCorrection(BEANS_KIT, RICE, 4, ALL);

        assertEquals(new StockChange.Amounts(6, 2), change.getKit());
    }

    @Test
    public void parseQuantity_acceptsOnlyWholeNumbersFrom0To9999() {
        assertEquals(Integer.valueOf(0), StockChange.parseQuantity("0"));
        assertEquals(Integer.valueOf(9999), StockChange.parseQuantity(" 9999 "));
        assertNull(StockChange.parseQuantity(""));
        assertNull(StockChange.parseQuantity("10000"));
        assertNull(StockChange.parseQuantity("-1"));
        assertNull(StockChange.parseQuantity("+5"));
        assertNull(StockChange.parseQuantity("2.5"));
        assertNull(StockChange.parseQuantity("six"));
        assertNull(StockChange.parseQuantity(null));
    }

    @Test
    public void parsePartAmount_acceptsOnlyOneToTheAvailableAmount() {
        assertEquals(Integer.valueOf(1), StockChange.parsePartAmount("1", 4));
        assertEquals(Integer.valueOf(4), StockChange.parsePartAmount("4", 4));
        assertNull(StockChange.parsePartAmount("0", 4));
        assertNull(StockChange.parsePartAmount("5", 4));
        assertNull(StockChange.parsePartAmount(" ", 4));
    }

    private static BatchInfo batch(long id, long productId, int quantity, long locationId,
                                   boolean archived) {
        boolean kit = locationId == KIT_ID;
        return new BatchInfo(id, productId, productId == BEANS ? "Canned beans" : "Rice", "",
                "", "cans", ItemCategory.FOOD, quantity, null, null, DateLabel.BEST_BY, false,
                locationId, kit ? "Earthquake kit" : "Pantry",
                kit ? LocationType.EMERGENCY_KIT : LocationType.PANTRY,
                null, null, null, null, null, archived, false);
    }
}

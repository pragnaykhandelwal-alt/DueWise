package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;

import com.example.readyshelf.domain.InventoryFilter.DateOrder;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tests for the Inventory screen's search, filters, and date order. */
public class InventoryFilterTest {

    private static final long PANTRY_ID = 1;
    private static final long KIT_ID = 2;

    private static final BatchInfo BARS_KIT = batch(1, "Granola bars", ItemCategory.FOOD,
            LocalDate.of(2026, 10, 12), KIT_ID);
    private static final BatchInfo WATER_KIT = batch(2, "Bottled water", ItemCategory.WATER,
            LocalDate.of(2027, 8, 1), KIT_ID);
    private static final BatchInfo BEANS_PANTRY = batch(3, "Canned beans", ItemCategory.FOOD,
            LocalDate.of(2026, 10, 1), PANTRY_ID);
    private static final BatchInfo RICE_NO_DATE = batch(4, "Rice", ItemCategory.FOOD,
            null, PANTRY_ID);

    private static final List<BatchInfo> ALL =
            Arrays.asList(BARS_KIT, WATER_KIT, BEANS_PANTRY, RICE_NO_DATE);

    @Test
    public void blankSearchAndNoFilters_keepEveryBatch() {
        List<BatchInfo> result = InventoryFilter.apply(ALL, "   ", null, null, DateOrder.SOONEST_FIRST);

        assertEquals(4, result.size());
    }

    @Test
    public void search_ignoresCapitalLettersAndOuterSpaces() {
        List<BatchInfo> result = InventoryFilter.apply(ALL, "  GRANOLA ", null, null, DateOrder.SOONEST_FIRST);

        assertEquals(Arrays.asList(BARS_KIT), result);
    }

    @Test
    public void search_alsoMatchesTheVariant() {
        BatchInfo lowSodium = new BatchInfo(5, 9, "Canned beans", "Low sodium", "15 oz", "cans",
                ItemCategory.FOOD, 3, null, null, DateLabel.BEST_BY, false, PANTRY_ID, "Pantry",
                LocationType.PANTRY, null, null, null, null, null, false, false);
        List<BatchInfo> batches = Arrays.asList(BARS_KIT, BEANS_PANTRY, lowSodium);

        List<BatchInfo> result = InventoryFilter.apply(batches, "sodium", null, null, DateOrder.SOONEST_FIRST);

        assertEquals(Arrays.asList(lowSodium), result);
    }

    @Test
    public void locationFilter_keepsOnlyThatLocation() {
        List<BatchInfo> result = InventoryFilter.apply(ALL, "", KIT_ID, null, DateOrder.SOONEST_FIRST);

        assertEquals(Arrays.asList(BARS_KIT, WATER_KIT), result);
    }

    @Test
    public void categoryFilter_keepsOnlyThatCategory() {
        List<BatchInfo> result = InventoryFilter.apply(ALL, "", null, ItemCategory.WATER, DateOrder.SOONEST_FIRST);

        assertEquals(Arrays.asList(WATER_KIT), result);
    }

    @Test
    public void soonestFirst_putsBatchesWithoutDateLast() {
        List<BatchInfo> result = InventoryFilter.apply(ALL, "", null, null, DateOrder.SOONEST_FIRST);

        assertEquals(Arrays.asList(BEANS_PANTRY, BARS_KIT, WATER_KIT, RICE_NO_DATE), result);
    }

    @Test
    public void latestFirst_stillPutsBatchesWithoutDateLast() {
        List<BatchInfo> result = InventoryFilter.apply(ALL, "", null, null, DateOrder.LATEST_FIRST);

        assertEquals(Arrays.asList(WATER_KIT, BARS_KIT, BEANS_PANTRY, RICE_NO_DATE), result);
    }

    @Test
    public void sameDate_sortedByNameIgnoringCapitalLetters() {
        LocalDate sameDay = LocalDate.of(2026, 12, 1);
        BatchInfo zucchini = batch(10, "zucchini", ItemCategory.FOOD, sameDay, PANTRY_ID);
        BatchInfo apples = batch(11, "Apples", ItemCategory.FOOD, sameDay, PANTRY_ID);

        List<BatchInfo> result = InventoryFilter.apply(Arrays.asList(zucchini, apples), "", null, null,
                DateOrder.SOONEST_FIRST);

        assertEquals(Arrays.asList(apples, zucchini), result);
    }

    @Test
    public void apply_doesNotChangeTheInputList() {
        List<BatchInfo> input = new ArrayList<>(ALL);

        InventoryFilter.apply(input, "", null, null, DateOrder.LATEST_FIRST);

        assertEquals(ALL, input);
    }

    // The status filter. The statuses are given here as a map; in the app they come from the
    // date rules. These tests don't use the date rules.

    @Test
    public void allStatuses_keepsEveryBatchInOrder() {
        Map<Long, DateStatus> statuses = new HashMap<>();
        statuses.put(BARS_KIT.getId(), DateStatus.NEAR_DATE);
        statuses.put(WATER_KIT.getId(), DateStatus.LATER);

        List<BatchInfo> result = InventoryFilter.withStatus(ALL, statuses, null);

        assertEquals(ALL, result);
    }

    @Test
    public void statusFilter_keepsOnlyThatStatusInOrder() {
        Map<Long, DateStatus> statuses = new HashMap<>();
        statuses.put(BARS_KIT.getId(), DateStatus.NEAR_DATE);
        statuses.put(WATER_KIT.getId(), DateStatus.LATER);
        statuses.put(BEANS_PANTRY.getId(), DateStatus.NEAR_DATE);
        statuses.put(RICE_NO_DATE.getId(), DateStatus.NEEDS_REVIEW);

        List<BatchInfo> result = InventoryFilter.withStatus(ALL, statuses, DateStatus.NEAR_DATE);

        assertEquals(Arrays.asList(BARS_KIT, BEANS_PANTRY), result);
    }

    @Test
    public void statusFilter_leavesOutBatchesWithoutStatus() {
        // Only the granola bars have a status, like a list where the others are archived.
        Map<Long, DateStatus> statuses = new HashMap<>();
        statuses.put(BARS_KIT.getId(), DateStatus.PAST_DATE);

        List<BatchInfo> result = InventoryFilter.withStatus(ALL, statuses, DateStatus.PAST_DATE);

        assertEquals(Arrays.asList(BARS_KIT), result);
    }

    @Test
    public void statusFilter_doesNotChangeTheInputList() {
        List<BatchInfo> input = new ArrayList<>(ALL);
        Map<Long, DateStatus> statuses = new HashMap<>();
        statuses.put(WATER_KIT.getId(), DateStatus.LATER);

        InventoryFilter.withStatus(input, statuses, DateStatus.LATER);

        assertEquals(ALL, input);
    }

    private static BatchInfo batch(long id, String name, ItemCategory category, LocalDate date,
                                   long locationId) {
        String locationName = locationId == KIT_ID ? "Earthquake kit" : "Pantry";
        LocationType locationType = locationId == KIT_ID ? LocationType.EMERGENCY_KIT : LocationType.PANTRY;
        return new BatchInfo(id, name, category, 1, "", date, DateLabel.BEST_BY,
                locationId, locationName, locationType, false);
    }
}

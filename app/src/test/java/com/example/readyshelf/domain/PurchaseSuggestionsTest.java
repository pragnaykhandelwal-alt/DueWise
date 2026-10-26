package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for PurchaseSuggestions: needs added up for each product and kit.
 * The needs here are made up by each test; no swap rules are involved.
 */
public class PurchaseSuggestionsTest {

    private static final long BEANS = 3;
    private static final long RICE = 4;
    private static final long QUAKE_KIT = 2;
    private static final long CAR_KIT = 5;

    @Test
    public void needsOfOneProductInOneKit_becomeOneSuggestion_withTheirSum() {
        BatchInfo beansA = batch(10, BEANS, "Canned beans", 5, QUAKE_KIT, "Earthquake kit",
                LocationType.EMERGENCY_KIT);
        BatchInfo beansB = batch(11, BEANS, "Canned beans", 4, QUAKE_KIT, "Earthquake kit",
                LocationType.EMERGENCY_KIT);
        RuleRunner.ShownNeed needA = need(beansA, 5);
        RuleRunner.ShownNeed needB = need(beansB, 2);

        List<PurchaseSuggestions.Suggestion> suggestions =
                PurchaseSuggestions.from(Arrays.asList(needA, needB));

        assertEquals(1, suggestions.size());
        PurchaseSuggestions.Suggestion suggestion = suggestions.get(0);
        assertEquals(new PurchaseSuggestions.Key(BEANS, QUAKE_KIT), suggestion.getKey());
        assertEquals(7, suggestion.getQuantity());
        assertEquals(Arrays.asList(needA, needB), suggestion.getNeeds());
        assertEquals(beansA, suggestion.getFirstBatch());
    }

    @Test
    public void theSameProductInTwoKits_orTwoProducts_giveSeparateSuggestions_sortedByNameThenKit() {
        RuleRunner.ShownNeed riceQuake = need(batch(20, RICE, "Rice", 2, QUAKE_KIT,
                "Earthquake kit", LocationType.EMERGENCY_KIT), 2);
        RuleRunner.ShownNeed beansQuake = need(batch(21, BEANS, "Canned beans", 3, QUAKE_KIT,
                "Earthquake kit", LocationType.EMERGENCY_KIT), 3);
        RuleRunner.ShownNeed beansCar = need(batch(22, BEANS, "Canned beans", 1, CAR_KIT,
                "Car kit", LocationType.EMERGENCY_KIT), 1);

        List<PurchaseSuggestions.Suggestion> suggestions =
                PurchaseSuggestions.from(Arrays.asList(riceQuake, beansQuake, beansCar));

        assertEquals(Arrays.asList(new PurchaseSuggestions.Key(BEANS, CAR_KIT),
                        new PurchaseSuggestions.Key(BEANS, QUAKE_KIT),
                        new PurchaseSuggestions.Key(RICE, QUAKE_KIT)),
                Arrays.asList(suggestions.get(0).getKey(), suggestions.get(1).getKey(),
                        suggestions.get(2).getKey()));
    }

    @Test
    public void theKeyStaysTheSame_whileTheAmountFollowsTheCurrentNeeds() {
        BatchInfo beans = batch(10, BEANS, "Canned beans", 5, QUAKE_KIT, "Earthquake kit",
                LocationType.EMERGENCY_KIT);
        BatchInfo fewerBeans = batch(10, BEANS, "Canned beans", 3, QUAKE_KIT, "Earthquake kit",
                LocationType.EMERGENCY_KIT);

        PurchaseSuggestions.Suggestion before =
                PurchaseSuggestions.from(Collections.singletonList(need(beans, 5))).get(0);
        // After stock changed (2 cans recorded as used), the need is made again from the batch.
        PurchaseSuggestions.Suggestion after =
                PurchaseSuggestions.from(Collections.singletonList(need(fewerBeans, 3))).get(0);

        assertEquals(before.getKey(), after.getKey());
        assertEquals(5, before.getQuantity());
        assertEquals(3, after.getQuantity());
    }

    @Test
    public void aPartlyReplacedNeedOfFour_afterASwapOfTwoFromSix_becomesASuggestionOfFour() {
        BatchInfo kitSix = batch(50, BEANS, "Canned beans", 6, QUAKE_KIT, "Earthquake kit",
                LocationType.EMERGENCY_KIT);
        RuleRunner.ShownNeed need = new RuleRunner.ShownNeed(kitSix, PlanReason.PARTLY_REPLACED, 4);

        List<PurchaseSuggestions.Suggestion> suggestions =
                PurchaseSuggestions.from(Collections.singletonList(need));

        assertEquals(1, suggestions.size());
        assertEquals(new PurchaseSuggestions.Key(BEANS, QUAKE_KIT), suggestions.get(0).getKey());
        assertEquals(4, suggestions.get(0).getQuantity());
    }

    @Test
    public void needsInAnyPlace_areAddedUp_butANeedOfZeroIsNot() {
        // Since 2026-10-22 RuleRunner also gives needs for expired items outside kits, and it
        // alone decides which batches have a need; until then only food in kits was added up.
        BatchInfo pantryBeans = batch(30, BEANS, "Canned beans", 5, 1, "Pantry", LocationType.PANTRY);
        BatchInfo pantryRice = batch(31, RICE, "Rice", 2, 1, "Pantry", LocationType.PANTRY);

        List<PurchaseSuggestions.Suggestion> totals = PurchaseSuggestions.from(
                Arrays.asList(need(pantryBeans, 5), need(pantryRice, 0)));

        assertEquals(1, totals.size());
        assertEquals(new PurchaseSuggestions.Key(BEANS, 1), totals.get(0).getKey());
        assertEquals(5, totals.get(0).getQuantity());
    }

    @Test
    public void theAmountIsNeverMoreThan9999() {
        RuleRunner.ShownNeed big1 = need(batch(40, BEANS, "Canned beans", 9999, QUAKE_KIT,
                "Earthquake kit", LocationType.EMERGENCY_KIT), 9999);
        RuleRunner.ShownNeed big2 = need(batch(41, BEANS, "Canned beans", 9999, QUAKE_KIT,
                "Earthquake kit", LocationType.EMERGENCY_KIT), 9999);

        assertEquals(StockChange.MAX_QUANTITY,
                PurchaseSuggestions.from(Arrays.asList(big1, big2)).get(0).getQuantity());
    }

    @Test
    public void stateOf_comparesASuggestionWithTheListByKey() {
        PurchaseSuggestions.Suggestion suggestion = PurchaseSuggestions.from(Collections.singletonList(
                need(batch(10, BEANS, "Canned beans", 5, QUAKE_KIT, "Earthquake kit",
                        LocationType.EMERGENCY_KIT), 5))).get(0);
        Map<PurchaseSuggestions.Key, Integer> listed = new HashMap<>();

        assertEquals(PurchaseSuggestions.ListState.NOT_ON_LIST,
                PurchaseSuggestions.stateOf(suggestion, listed));
        listed.put(new PurchaseSuggestions.Key(BEANS, QUAKE_KIT), 5);
        assertEquals(PurchaseSuggestions.ListState.ON_LIST_SAME,
                PurchaseSuggestions.stateOf(suggestion, listed));
        listed.put(new PurchaseSuggestions.Key(BEANS, QUAKE_KIT), 2);
        assertEquals(PurchaseSuggestions.ListState.ON_LIST_OTHER_QUANTITY,
                PurchaseSuggestions.stateOf(suggestion, listed));
    }

    private static RuleRunner.ShownNeed need(BatchInfo batch, int stillNeeded) {
        return new RuleRunner.ShownNeed(batch, PlanReason.DATE_TODAY_OR_PASSED, stillNeeded);
    }

    private static BatchInfo batch(long id, long productId, String name, int quantity,
                                   long locationId, String locationName, LocationType type) {
        return new BatchInfo(id, productId, name, "", "", "cans", null, ItemCategory.FOOD,
                quantity, LocalDate.of(2026, 10, 1), null, DateLabel.BEST_BY, true, locationId,
                locationName, type, null, null, null, null, null, false, false);
    }
}

package com.example.readyshelf.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.ConditionAnswer;
import com.example.readyshelf.domain.DateEntry;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.FoodClass;
import com.example.readyshelf.domain.FoodType;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.PlanBatch;
import com.example.readyshelf.domain.PlanNote;
import com.example.readyshelf.domain.PlanReason;
import com.example.readyshelf.domain.PurchaseSuggestions;
import com.example.readyshelf.domain.RotationPlan;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.domain.RuleRunner;
import com.example.readyshelf.domain.StockChange;
import com.example.readyshelf.domain.SwapPreview;
import com.example.readyshelf.domain.SwapProposal;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Tests for the made-up sample data: which batches it has, their dates counted from the day it
 * is opened, and that the app's own checks let it show a swap and a purchase suggestion once
 * the student's rules are connected.
 *
 * <p>These tests don't test any swap rules. The planner below returns answers that each test
 * sets up in advance and never looks at the batches; it is not a RotationPlanner.
 */
public class SampleDataTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final long PANTRY_ID = 11;
    private static final long KIT_ID = 12;
    private static final long FIRST_AID_ID = 13;

    private final List<SampleData.Item> items =
            SampleData.create(TODAY, PANTRY_ID, KIT_ID, FIRST_AID_ID);

    // ----- The batches -----

    @Test
    public void granolaInTheKitAndThePantry_isOneShelfStableProductCountedInBars() {
        SampleData.Item kit = kitGranola(items);
        SampleData.Item pantry = pantryGranola(items);

        assertSame(kit.product, pantry.product);
        assertEquals("bars", kit.product.unit);
        assertEquals(FoodClass.SHELF_STABLE_FOOD, kit.product.foodClass);
        assertEquals(6, kit.batch.quantity);
        assertEquals(6, pantry.batch.quantity);
    }

    @Test
    public void granolaInTheKit_isDated20DaysAway_andThePantryGranola240_bothConfirmedBestBy() {
        Batch kit = kitGranola(items).batch;
        Batch pantry = pantryGranola(items).batch;

        assertEquals(LocalDate.of(2026, 11, 1), kit.date);
        assertEquals(LocalDate.of(2027, 6, 9), pantry.date);
        for (Batch batch : new Batch[] {kit, pantry}) {
            assertEquals(DateLabel.BEST_BY, batch.dateLabel);
            assertTrue(batch.dateConfirmed);
            assertNull(batch.printedDateText);
            assertEquals(ItemCategory.FOOD, batch.category);
        }
    }

    @Test
    public void theThirdGranolaBatch_isInThePantry_withOnlyAMonthAndYear_notConfirmed() {
        SampleData.Item incomplete = only(items, item -> item.product.name.equals("Granola bars")
                && item.batch.date == null);
        Batch batch = incomplete.batch;

        assertSame(kitGranola(items).product, incomplete.product);
        assertEquals(PANTRY_ID, batch.locationId);
        assertEquals(3, batch.quantity);
        assertEquals("07/2027", batch.printedDateText);
        assertFalse(batch.dateConfirmed);
        assertEquals(DateEntry.Kind.INCOMPLETE, DateEntry.kindOf(batch.date, batch.printedDateText));
        // So the batch form can offer "Use the last day of the month (31 Jul 2027)".
        assertEquals(LocalDate.of(2027, 7, 31), DateEntry.lastDayOfPrintedMonth(batch.printedDateText));
    }

    @Test
    public void theBeansInTheKit_areDated14DaysAway_andNoOtherBatchIsTheirProduct() {
        SampleData.Item beans = only(items, item -> item.product.name.equals("Canned beans"));

        assertEquals(KIT_ID, beans.batch.locationId);
        assertEquals(4, beans.batch.quantity);
        assertEquals("cans", beans.product.unit);
        assertEquals(FoodClass.SHELF_STABLE_FOOD, beans.product.foodClass);
        assertEquals(LocalDate.of(2026, 10, 26), beans.batch.date);
        assertTrue(beans.batch.dateConfirmed);
        assertEquals(FoodType.CANNED, beans.batch.foodType);
        only(items, item -> item.product == beans.product);
    }

    @Test
    public void everyFoodBatch_hasTheAnswersThatSwapsNeed() {
        int food = 0;
        for (SampleData.Item item : items) {
            if (item.batch.category == ItemCategory.FOOD) {
                food++;
                assertEquals(ConditionAnswer.YES, item.batch.sealed);
                assertEquals(ConditionAnswer.YES, item.batch.shelfStable);
                assertEquals(ConditionAnswer.NO, item.batch.damaged);
                assertEquals(ConditionAnswer.NO, item.batch.storageConcern);
                assertEquals(FoodClass.SHELF_STABLE_FOOD, item.product.foodClass);
            }
        }
        assertEquals(4, food);
    }

    @Test
    public void theMedicine_inTheFirstAidBox_expired10DaysAgo_withNoFoodAnswersOrFoodClass() {
        SampleData.Item medicine = only(items, item -> item.batch.category == ItemCategory.MEDICINE);
        Batch batch = medicine.batch;

        assertEquals(FIRST_AID_ID, batch.locationId);
        assertEquals(2, batch.quantity);
        assertEquals(LocalDate.of(2026, 10, 2), batch.date);
        assertEquals(DateLabel.EXPIRES, batch.dateLabel);
        assertTrue(batch.dateConfirmed);
        assertNull(medicine.product.foodClass);
        assertNull(batch.foodType);
        assertNull(batch.sealed);
        assertNull(batch.shelfStable);
        assertNull(batch.damaged);
        assertNull(batch.storageConcern);
    }

    @Test
    public void everyRow_isMarkedSample_andNoNameCallsFoodSafe() {
        assertEquals(5, items.size());
        for (SampleData.Item item : items) {
            assertTrue(item.product.sample);
            assertTrue(item.batch.sample);
            assertFalse(item.batch.archived);
            String words = (item.product.name + " " + item.product.variant).toLowerCase(Locale.ROOT);
            assertFalse(words, words.contains("safe"));
        }
    }

    @Test
    public void datesAreCountedFromTheDayTheSampleDataIsOpened() {
        List<SampleData.Item> later =
                SampleData.create(LocalDate.of(2028, 2, 10), PANTRY_ID, KIT_ID, FIRST_AID_ID);

        assertEquals(LocalDate.of(2028, 3, 1), kitGranola(later).batch.date); // past 29 Feb 2028
        assertEquals(LocalDate.of(2028, 10, 7), pantryGranola(later).batch.date);
        assertEquals(LocalDate.of(2028, 1, 31),
                only(later, item -> item.batch.category == ItemCategory.MEDICINE).batch.date);
        assertEquals("11/2028", only(later, item -> item.batch.date == null).batch.printedDateText);
    }

    @Test
    public void monthAndYear_hasTwoDigitsForTheMonth_andCanEndAShortMonth() {
        assertEquals("07/2027", SampleData.monthAndYear(LocalDate.of(2027, 7, 12)));
        // 31 May 2026 + 9 months is 28 Feb 2027.
        assertEquals("02/2027", SampleData.monthAndYear(LocalDate.of(2026, 5, 31).plusMonths(9)));
    }

    // ----- Which files can be emptied -----

    @Test
    public void onlyASampleDataFileName_canBeEmptied_neverTheHouseholdsFile() {
        assertTrue(AppDatabase.isSampleDataFileName(AppDatabase.SAMPLE_FILE_NAME));
        assertTrue(AppDatabase.isSampleDataFileName("datasets-test-sample-data.db"));

        assertFalse(AppDatabase.isSampleDataFileName(AppDatabase.FILE_NAME));
        assertFalse(AppDatabase.isSampleDataFileName("readyshelf.db"));
        assertFalse(AppDatabase.isSampleDataFileName("migration-test.db"));
        assertFalse(AppDatabase.isSampleDataFileName("sample-data.db.bak"));
        assertFalse(AppDatabase.isSampleDataFileName(null)); // a database in memory
    }

    // ----- What the app's own checks do with the sample data (no swap rules) -----

    @Test
    public void appChecks_keepAPresetGranolaSwapOfSix_andEachPlaceKeepsItsTotal() {
        List<BatchInfo> batches = asStored(items);
        BatchInfo kit = find(batches, b -> b.getName().equals("Granola bars")
                && b.getLocationId() == KIT_ID);
        BatchInfo pantry = find(batches, b -> b.getName().equals("Granola bars")
                && b.getLocationId() == PANTRY_ID && b.getDate() != null);
        PresetPlanner planner = new PresetPlanner().proposal(kit.getId(), pantry.getId(), 6);

        List<RuleRunner.ShownSwap> swaps =
                RuleRunner.plan(batches, planner, TODAY, DateThresholds.defaults()).getSwaps();

        assertEquals(Collections.singletonList(new RuleRunner.ShownSwap(kit, pantry, 6)), swaps);
        SwapPreview preview = SwapPreview.of(swaps.get(0), batches);
        assertEquals(new StockChange.Amounts(6, 6), preview.getKitTotal());
        // 6 bars dated 240 days away and 3 with an incomplete date: 9 before and after.
        assertEquals(new StockChange.Amounts(9, 9), preview.getPantryTotal());
    }

    @Test
    public void appChecks_dropAPresetSwapThatUsesTheIncompleteDateBatch() {
        List<BatchInfo> batches = asStored(items);
        BatchInfo kit = find(batches, b -> b.getName().equals("Granola bars")
                && b.getLocationId() == KIT_ID);
        BatchInfo incomplete = find(batches, b -> b.getDate() == null);
        PresetPlanner planner = new PresetPlanner().proposal(kit.getId(), incomplete.getId(), 3);

        assertTrue(RuleRunner.plan(batches, planner, TODAY, DateThresholds.defaults())
                .getSwaps().isEmpty());
    }

    @Test
    public void aPresetNeedOfFourBeans_becomesOneSampleSuggestionOfFourCansForTheKit() {
        List<BatchInfo> batches = asStored(items);
        BatchInfo beans = find(batches, b -> b.getName().equals("Canned beans"));
        PresetPlanner planner = new PresetPlanner()
                .note(beans.getId(), PlanReason.NO_REPLACEMENT, 4);

        List<RuleRunner.ShownNeed> needs =
                RuleRunner.plan(batches, planner, TODAY, DateThresholds.defaults()).getNeeds();
        List<PurchaseSuggestions.Suggestion> suggestions = PurchaseSuggestions.from(needs);

        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(beans, PlanReason.NO_REPLACEMENT, 4)), needs);
        assertEquals(1, suggestions.size());
        assertEquals(4, suggestions.get(0).getQuantity());
        assertEquals(new PurchaseSuggestions.Key(beans.getProductId(), KIT_ID),
                suggestions.get(0).getKey());
        // Sample suggestions show "Sample items can't be added to the purchase list."
        assertTrue(suggestions.get(0).isSample());
    }

    @Test
    public void aPresetNoteOnTheMedicine_neverBecomesANeed() {
        List<BatchInfo> batches = asStored(items);
        BatchInfo medicine = find(batches, b -> b.getCategory() == ItemCategory.MEDICINE);
        PresetPlanner planner = new PresetPlanner()
                .note(medicine.getId(), PlanReason.NO_REPLACEMENT, 1);

        assertTrue(RuleRunner.plan(batches, planner, TODAY, DateThresholds.defaults())
                .getNeeds().isEmpty());
    }

    // ----- Test helpers -----

    private static SampleData.Item kitGranola(List<SampleData.Item> items) {
        return only(items, item -> item.product.name.equals("Granola bars")
                && item.batch.locationId == KIT_ID);
    }

    private static SampleData.Item pantryGranola(List<SampleData.Item> items) {
        return only(items, item -> item.product.name.equals("Granola bars")
                && item.batch.locationId == PANTRY_ID && item.batch.date != null);
    }

    /** The one item that matches; fails when none or more than one matches. */
    private static SampleData.Item only(List<SampleData.Item> items,
                                        Predicate<SampleData.Item> matches) {
        List<SampleData.Item> found = new ArrayList<>();
        for (SampleData.Item item : items) {
            if (matches.test(item)) {
                found.add(item);
            }
        }
        assertEquals(1, found.size());
        return found.get(0);
    }

    private static BatchInfo find(List<BatchInfo> batches, Predicate<BatchInfo> matches) {
        List<BatchInfo> found = new ArrayList<>();
        for (BatchInfo batch : batches) {
            if (matches.test(batch)) {
                found.add(batch);
            }
        }
        assertEquals(1, found.size());
        return found.get(0);
    }

    /**
     * The sample items as the screens and the rules get them from the database: each batch with
     * its product and location, with made-up ids. Items of one product get one product id.
     */
    private static List<BatchInfo> asStored(List<SampleData.Item> items) {
        Map<Product, Long> productIds = new IdentityHashMap<>();
        List<BatchInfo> batches = new ArrayList<>();
        long batchId = 1;
        for (SampleData.Item item : items) {
            Long productId = productIds.get(item.product);
            if (productId == null) {
                productId = 100L + productIds.size();
                productIds.put(item.product, productId);
            }
            Product product = item.product;
            Batch batch = item.batch;
            LocationType type = batch.locationId == PANTRY_ID ? LocationType.PANTRY
                    : batch.locationId == KIT_ID ? LocationType.EMERGENCY_KIT : LocationType.FIRST_AID;
            String place = type == LocationType.PANTRY ? "Pantry"
                    : type == LocationType.EMERGENCY_KIT ? "Emergency kit" : "First aid box";
            batches.add(new BatchInfo(batchId++, productId, product.name, product.variant,
                    product.packageSize, product.unit, product.foodClass, batch.category,
                    batch.quantity, batch.date, batch.printedDateText, batch.dateLabel,
                    batch.dateConfirmed, batch.locationId, place, type, batch.foodType,
                    batch.sealed, batch.shelfStable, batch.damaged, batch.storageConcern,
                    batch.archived, batch.sample));
        }
        return batches;
    }

    /**
     * Returns the plan each test sets up and never looks at the batches it is given, so it
     * contains no swap rules. Not a RotationPlanner.
     */
    private static final class PresetPlanner implements RotationPlannerContract {
        private final List<SwapProposal> proposals = new ArrayList<>();
        private final List<PlanNote> notes = new ArrayList<>();

        PresetPlanner proposal(long kitBatchId, long pantryBatchId, int quantity) {
            proposals.add(new SwapProposal(kitBatchId, pantryBatchId, quantity));
            return this;
        }

        PresetPlanner note(long batchId, PlanReason reason, int stillNeeded) {
            notes.add(new PlanNote(batchId, reason, stillNeeded));
            return this;
        }

        @Override
        public RotationPlan plan(List<PlanBatch> batches, LocalDate today, int nearDateDays,
                                 int replacementDays) {
            return new RotationPlan(proposals, notes);
        }
    }
}

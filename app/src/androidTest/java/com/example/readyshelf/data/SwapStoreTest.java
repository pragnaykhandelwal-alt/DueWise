package com.example.readyshelf.data;

import static com.example.readyshelf.data.StoreTestData.count;
import static com.example.readyshelf.data.StoreTestData.dump;
import static com.example.readyshelf.data.StoreTestData.foodBatch;
import static com.example.readyshelf.data.StoreTestData.location;
import static com.example.readyshelf.data.StoreTestData.product;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.data.StoreTestData.PresetPlanner;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.ConditionAnswer;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.FoodClass;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.domain.RuleRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage tests for confirmed swaps (SwapStore): partial and whole swaps, the categories that can
 * be swapped, stale proposals, and repeated confirmation. Each test uses a database in memory
 * with made-up rows.
 *
 * <p>The planners here only return answers that each test sets up (see StoreTestData). They are
 * not a RotationPlanner and contain no swap rules.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class SwapStoreTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);
    private static final DateThresholds SETTINGS = DateThresholds.of(30, 180);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AppDatabase db;
    private SwapStore store;
    private long pantryId;
    private long kitId;
    private long beansId;

    @Before
    public void createDatabase() {
        db = StoreTestData.inMemory(context);
        store = new SwapStore(db);
        pantryId = location(db, "Pantry", LocationType.PANTRY);
        kitId = location(db, "Earthquake kit", LocationType.EMERGENCY_KIT);
        beansId = product(db, "Canned beans", "cans", FoodClass.SHELF_STABLE_FOOD, false);
    }

    @After
    public void closeDatabase() {
        db.close();
    }

    @Test
    public void foreignKeysAreOn() {
        assertEquals("1", StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "PRAGMA foreign_keys").get(0));
    }

    // ----- Swaps that are saved -----

    @Test
    public void partialSwap_movesThreeEachWay_andEveryPartKeepsItsOwnDate() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));

        assertEquals(SwapStore.Result.SAVED, confirm(request, planner, DAY));

        // The kit batch keeps 2 in the kit; its 3 moved units are a new batch in the pantry.
        assertBatch(kitBatch, kitId, 2, DAY.plusDays(10));
        assertBatch(onlyBatchAt(pantryId, DAY.plusDays(10)), pantryId, 3, DAY.plusDays(10));
        // The pantry batch keeps 7 in the pantry; its 3 moved units are a new batch in the kit.
        assertBatch(pantryBatch, pantryId, 7, DAY.plusDays(250));
        assertBatch(onlyBatchAt(kitId, DAY.plusDays(250)), kitId, 3, DAY.plusDays(250));
        // Each moved part keeps the label type, confirmation, and food answers.
        Batch movedPart = db.batchDao().getById(onlyBatchAt(pantryId, DAY.plusDays(10)));
        assertNotNull(movedPart);
        assertTrue(movedPart.dateConfirmed);
        assertEquals(ConditionAnswer.YES, movedPart.sealed);
        assertEquals(ConditionAnswer.NO, movedPart.damaged);

        // History: one "Swapped" entry for each of the four batches, with the screen's texts.
        assertEquals(asList(
                        "SWAPPED|kit history", "SWAPPED|kit part history",
                        "SWAPPED|pantry history", "SWAPPED|pantry part history"),
                StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                        "SELECT type, details FROM batch_events ORDER BY details"));
        // The receipt.
        assertEquals(asList("action-1|" + kitBatch + "|" + pantryBatch + "|3|2026-10-11"),
                StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                        "SELECT actionId, kitBatchId, pantryBatchId, quantity, planDate FROM swap_actions"));
    }

    @Test
    public void wholeSwap_changesBothLocations_andMakesNoNewBatch() {
        long kitBatch = foodBatch(db, beansId, kitId, 3, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 3, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);

        assertEquals(SwapStore.Result.SAVED,
                confirm(request("action-1", shown(planner)), planner, DAY));

        assertBatch(kitBatch, pantryId, 3, DAY.plusDays(10));
        assertBatch(pantryBatch, kitId, 3, DAY.plusDays(250));
        assertEquals(2, count(db, "batches"));
        assertEquals(2, count(db, "batch_events"));
    }

    @Test
    public void mixedSwap_allOfTheKitBatch_partOfThePantryBatch() {
        long kitBatch = foodBatch(db, beansId, kitId, 3, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);

        assertEquals(SwapStore.Result.SAVED,
                confirm(request("action-1", shown(planner)), planner, DAY));

        assertBatch(kitBatch, pantryId, 3, DAY.plusDays(10));
        assertBatch(pantryBatch, pantryId, 7, DAY.plusDays(250));
        assertBatch(onlyBatchAt(kitId, DAY.plusDays(250)), kitId, 3, DAY.plusDays(250));
        assertEquals(3, count(db, "batches"));
    }

    @Test
    public void partialSwap_sixAndTwo_keepsFourInTheKit_movesAllOfThePantryBatch_andKeepsEightUnits() {
        long kitBatch = foodBatch(db, beansId, kitId, 6, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 2, DAY.plusDays(250), false);
        // A label type other than the default, so the test can see that the moved part keeps it.
        Batch kit = db.batchDao().getById(kitBatch);
        assertNotNull(kit);
        kit.dateLabel = DateLabel.USE_BY;
        db.batchDao().update(kit);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 2);

        assertEquals(SwapStore.Result.SAVED,
                confirm(request("action-1", shown(planner)), planner, DAY));

        // The kit batch keeps 4 with its date; its 2 moved units are a new batch in the pantry.
        assertBatch(kitBatch, kitId, 4, DAY.plusDays(10));
        long movedPart = onlyBatchAt(pantryId, DAY.plusDays(10));
        assertBatch(movedPart, pantryId, 2, DAY.plusDays(10));
        // All of the pantry batch moved into the kit, so no new batch was made for it.
        assertBatch(pantryBatch, kitId, 2, DAY.plusDays(250));
        assertEquals(3, count(db, "batches"));
        Batch moved = db.batchDao().getById(movedPart);
        assertNotNull(moved);
        assertEquals(DateLabel.USE_BY, moved.dateLabel);
        assertTrue(moved.dateConfirmed);
        assertEquals(ConditionAnswer.YES, moved.sealed);
        assertEquals(ConditionAnswer.NO, moved.damaged);
        assertEquals(ConditionAnswer.NO, moved.storageConcern);
        // No unit is lost or added: 8 in all, 6 in the kit and 2 in the pantry, as before.
        assertEquals(asList("8"), StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT SUM(quantity) FROM batches"));
        assertEquals(asList("6"), StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT SUM(quantity) FROM batches WHERE locationId = " + kitId));
        assertEquals(asList("2"), StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT SUM(quantity) FROM batches WHERE locationId = " + pantryId));
    }

    @Test
    public void twoSwapsShownForOnePantryBatch_onlyTheFirstConfirmedIsSaved() {
        long carKitId = location(db, "Car kit", LocationType.EMERGENCY_KIT);
        long quakeKitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long carKitBatch = foodBatch(db, beansId, carKitId, 5, DAY.plusDays(12), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 3, DAY.plusDays(250), false);
        // Two swaps shown before either was confirmed. Together they would need 6 of the
        // pantry batch's 3 units.
        PresetPlanner forQuakeKit = PresetPlanner.always(quakeKitBatch, pantryBatch, 3);
        PresetPlanner forCarKit = PresetPlanner.always(carKitBatch, pantryBatch, 3);
        SwapStore.Request first = request("action-A", shown(forQuakeKit));
        SwapStore.Request second = request("action-B", shown(forCarKit));

        assertEquals(SwapStore.Result.SAVED, confirm(first, forQuakeKit, DAY));
        List<String> afterFirst = dump(db);
        assertEquals(SwapStore.Result.BATCH_CHANGED, confirm(second, forCarKit, DAY));

        assertEquals(afterFirst, dump(db));
        assertEquals(1, count(db, "swap_actions"));
        // The pantry batch's 3 units went to the first kit only.
        assertBatch(pantryBatch, kitId, 3, DAY.plusDays(250));
        assertBatch(carKitBatch, carKitId, 5, DAY.plusDays(12));
    }

    // ----- Categories: since 2026-10-22 everything but medicine, documents, and subscriptions -----

    @Test
    public void aWaterSwap_isSaved_likeAFoodSwap() {
        long waterId = product(db, "Bottled water", "bottles", null, false);
        long kitBatch = StoreTestData.batch(db, waterId, kitId, ItemCategory.WATER, 6, DAY.plusDays(10));
        long pantryBatch = StoreTestData.batch(db, waterId, pantryId, ItemCategory.WATER, 6,
                DAY.plusDays(250));
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 6);

        assertEquals(SwapStore.Result.SAVED,
                confirm(request("action-1", shown(planner)), planner, DAY));

        assertBatch(kitBatch, pantryId, 6, DAY.plusDays(10));
        assertBatch(pantryBatch, kitId, 6, DAY.plusDays(250));
        assertEquals(1, count(db, "swap_actions"));
    }

    @Test
    public void aMedicineDocumentOrSubscriptionSwap_isNeverShown_andSavesNothingIfAsked() {
        for (ItemCategory category : new ItemCategory[] {
                ItemCategory.MEDICINE, ItemCategory.DOCUMENT, ItemCategory.SUBSCRIPTION}) {
            long productId = product(db, "Test " + category.name(), "units", null, false);
            long kitBatch = StoreTestData.batch(db, productId, kitId, category, 2, DAY.plusDays(10));
            long pantryBatch = StoreTestData.batch(db, productId, pantryId, category, 2,
                    DAY.plusDays(250));
            // The preset answer proposes the swap; the app's safety check drops it.
            PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 2);
            assertEquals(category.name(), 0, RuleRunner.plan(db.batchDao().getActiveInfo(),
                    planner, DAY, SETTINGS).getSwaps().size());

            // No screen can ask for this swap. Asked anyway, it saves nothing.
            RuleRunner.ShownSwap asked = new RuleRunner.ShownSwap(info(kitBatch), info(pantryBatch), 2);
            List<String> before = dump(db);
            assertEquals(category.name(), SwapStore.Result.NO_LONGER_PROPOSED,
                    confirm(request("action-" + category.name(), asked), planner, DAY));
            assertEquals(category.name(), before, dump(db));
        }
        assertEquals(0, count(db, "swap_actions"));
    }

    // ----- Stale proposals: nothing is saved -----

    @Test
    public void stalePantryBatch_savesNothing() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));
        // 2 cans of the pantry batch are recorded as used after the swap was shown.
        Batch pantry = db.batchDao().getById(pantryBatch);
        assertNotNull(pantry);
        pantry.quantity = 8;
        db.batchDao().update(pantry);
        List<String> before = dump(db);

        assertEquals(SwapStore.Result.BATCH_CHANGED, confirm(request, planner, DAY));
        assertEquals(before, dump(db));
        assertEquals(0, count(db, "swap_actions"));
    }

    @Test
    public void staleQuantity_savesNothing() {
        assertStaleAfter(batch -> batch.quantity = 4, SwapStore.Result.BATCH_CHANGED);
    }

    @Test
    public void staleDate_savesNothing() {
        assertStaleAfter(batch -> batch.date = DAY.plusDays(12), SwapStore.Result.BATCH_CHANGED);
    }

    @Test
    public void staleProduct_savesNothing() {
        long otherProduct = product(db, "Canned corn", "cans", FoodClass.SHELF_STABLE_FOOD, false);
        assertStaleAfter(batch -> batch.productId = otherProduct, SwapStore.Result.BATCH_CHANGED);
    }

    @Test
    public void staleLocation_savesNothing() {
        long carKit = location(db, "Car kit", LocationType.EMERGENCY_KIT);
        assertStaleAfter(batch -> batch.locationId = carKit, SwapStore.Result.BATCH_CHANGED);
    }

    @Test
    public void staleCondition_savesNothing() {
        assertStaleAfter(batch -> batch.damaged = ConditionAnswer.YES, SwapStore.Result.BATCH_CHANGED);
    }

    @Test
    public void archivedBatch_savesNothing() {
        assertStaleAfter(batch -> batch.archived = true, SwapStore.Result.BATCH_CHANGED);
    }

    @Test
    public void staleFoodClass_savesNothing() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));
        db.productDao().setFoodClass(beansId, FoodClass.PERISHABLE_FOOD);
        List<String> before = dump(db);

        assertEquals(SwapStore.Result.BATCH_CHANGED, confirm(request, planner, DAY));
        assertEquals(before, dump(db));
    }

    @Test
    public void afterMidnight_whenTheRulesNoLongerProposeIt_savesNothing() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        // The preset answer has the swap only on DAY (the records don't change).
        PresetPlanner planner = new PresetPlanner((today, near, replacement) -> today.equals(DAY)
                ? StoreTestData.plan(kitBatch, pantryBatch, 3) : StoreTestData.emptyPlan());
        SwapStore.Request request = request("action-1", shown(planner));
        List<String> before = dump(db);

        assertEquals(SwapStore.Result.NO_LONGER_PROPOSED, confirm(request, planner, DAY.plusDays(1)));
        assertEquals(DAY.plusDays(1), planner.lastToday);
        assertEquals(before, dump(db));
    }

    @Test
    public void afterMidnight_whenTheKitBatchIsNowDatedToday_savesNothing() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(1), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        // The same answer every day; the app's safety check drops it once the kit date is today.
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));
        List<String> before = dump(db);

        assertEquals(SwapStore.Result.NO_LONGER_PROPOSED, confirm(request, planner, DAY.plusDays(1)));
        assertEquals(before, dump(db));
    }

    @Test
    public void afterMidnight_whenTheRulesStillProposeIt_isSaved() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));

        assertEquals(SwapStore.Result.SAVED, confirm(request, planner, DAY.plusDays(1)));
        assertEquals(asList("2026-10-12"), StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT planDate FROM swap_actions"));
    }

    @Test
    public void afterASettingsChange_whenTheRulesNoLongerProposeIt_savesNothing() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        // The preset answer has the swap only with 30 near-date days and 180 replacement days.
        PresetPlanner planner = new PresetPlanner((today, near, replacement) ->
                near == 30 && replacement == 180
                        ? StoreTestData.plan(kitBatch, pantryBatch, 3) : StoreTestData.emptyPlan());
        SwapStore.Request request = request("action-1", shown(planner));
        List<String> before = dump(db);

        SwapStore.Result result = store.confirm(request, planner, () -> DAY,
                () -> DateThresholds.of(30, 300));

        assertEquals(SwapStore.Result.NO_LONGER_PROPOSED, result);
        assertEquals(300, planner.lastReplacementDays);
        assertEquals(before, dump(db));
    }

    // ----- Repeated confirmation and failures -----

    @Test
    public void duplicateConfirmation_withTheSameActionId_movesTheStockOnce() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));

        assertEquals(SwapStore.Result.SAVED, confirm(request, planner, DAY));
        List<String> afterFirst = dump(db);
        assertEquals(SwapStore.Result.ALREADY_SAVED, confirm(request, planner, DAY));
        assertEquals(SwapStore.Result.ALREADY_SAVED, confirm(request, planner, DAY));

        assertEquals(afterFirst, dump(db));
        assertBatch(kitBatch, kitId, 2, DAY.plusDays(10));
        assertBatch(pantryBatch, pantryId, 7, DAY.plusDays(250));
        assertEquals(1, count(db, "swap_actions"));
    }

    @Test
    public void anActionIdUsedForAnotherSwap_savesNothing() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner three = PresetPlanner.always(kitBatch, pantryBatch, 3);
        assertEquals(SwapStore.Result.SAVED, confirm(request("action-1", shown(three)), three, DAY));
        PresetPlanner one = PresetPlanner.always(kitBatch, pantryBatch, 1);
        SwapStore.Request reused = request("action-1", shown(one));
        List<String> before = dump(db);

        assertEquals(SwapStore.Result.FAILED, confirm(reused, one, DAY));
        assertEquals(before, dump(db));
    }

    @Test
    public void whenTheRulesThrow_savesNothing() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        SwapStore.Request request = request("action-1",
                shown(PresetPlanner.always(kitBatch, pantryBatch, 3)));
        RotationPlannerContract throwing = new PresetPlanner((today, near, replacement) -> {
            throw new IllegalStateException("made-up error");
        });
        List<String> before = dump(db);

        assertEquals(SwapStore.Result.FAILED, confirm(request, throwing, DAY));
        assertEquals(before, dump(db));
    }

    @Test
    public void aFailureAfterBothMovements_savesNeitherMovement_andTheSameActionIdCanTryAgain() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));
        List<String> before = dump(db);
        // The receipt is the last write, after both movements and their history. This test
        // table rule makes it fail, as if the phone's storage had a problem at that moment.
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER fail_swap_record "
                + "BEFORE INSERT ON swap_actions BEGIN SELECT RAISE(ABORT, 'made-up failure'); END");

        assertEquals(SwapStore.Result.FAILED, confirm(request, planner, DAY));
        assertEquals(before, dump(db));

        db.getOpenHelper().getWritableDatabase().execSQL("DROP TRIGGER fail_swap_record");
        assertEquals(SwapStore.Result.SAVED, confirm(request, planner, DAY));
        assertEquals(SwapStore.Result.ALREADY_SAVED, confirm(request, planner, DAY));
        assertBatch(kitBatch, kitId, 2, DAY.plusDays(10));
        assertBatch(pantryBatch, pantryId, 7, DAY.plusDays(250));
    }

    @Test
    public void aFailureDuringTheSecondMovement_savesNeitherMovement() {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));
        List<String> before = dump(db);
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER fail_pantry_move "
                + "BEFORE UPDATE ON batches WHEN OLD.id = " + pantryBatch
                + " BEGIN SELECT RAISE(ABORT, 'made-up failure'); END");

        assertEquals(SwapStore.Result.FAILED, confirm(request, planner, DAY));
        assertEquals(before, dump(db));
    }

    @Test
    public void removingSampleData_afterASampleSwap_removesItsReceiptToo() {
        long sampleBeans = product(db, "Sample beans", "cans", FoodClass.SHELF_STABLE_FOOD, true);
        long kitBatch = foodBatch(db, sampleBeans, kitId, 5, DAY.plusDays(10), true);
        long pantryBatch = foodBatch(db, sampleBeans, pantryId, 10, DAY.plusDays(250), true);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        assertEquals(SwapStore.Result.SAVED,
                confirm(request("action-1", shown(planner)), planner, DAY));

        db.runInTransaction(() -> {
            db.batchDao().deleteSample();
            db.productDao().deleteUnusedSample();
        });

        assertEquals(0, count(db, "swap_actions"));
        assertEquals(0, count(db, "batches"));
        assertEquals(new ArrayList<String>(), StoreTestData.rows(
                db.getOpenHelper().getReadableDatabase(), "PRAGMA foreign_key_check"));
    }

    // ----- Helpers -----

    /** Changes the kit batch after the swap was shown, then checks that nothing is saved. */
    private void assertStaleAfter(@NonNull BatchChange change, @NonNull SwapStore.Result expected) {
        long kitBatch = foodBatch(db, beansId, kitId, 5, DAY.plusDays(10), false);
        long pantryBatch = foodBatch(db, beansId, pantryId, 10, DAY.plusDays(250), false);
        PresetPlanner planner = PresetPlanner.always(kitBatch, pantryBatch, 3);
        SwapStore.Request request = request("action-1", shown(planner));
        Batch batch = db.batchDao().getById(kitBatch);
        assertNotNull(batch);
        change.apply(batch);
        db.batchDao().update(batch);
        List<String> before = dump(db);

        assertEquals(expected, confirm(request, planner, DAY));
        assertEquals(before, dump(db));
        assertEquals(0, count(db, "swap_actions"));
    }

    private interface BatchChange {
        void apply(@NonNull Batch batch);
    }

    /** The swap as the Review swap screen would show it: from a plan made on DAY. */
    @NonNull
    private RuleRunner.ShownSwap shown(@NonNull RotationPlannerContract planner) {
        List<RuleRunner.ShownSwap> swaps = RuleRunner.plan(db.batchDao().getActiveInfo(), planner,
                DAY, SETTINGS).getSwaps();
        assertEquals(1, swaps.size());
        return swaps.get(0);
    }

    @NonNull
    private static SwapStore.Request request(@NonNull String actionId,
                                             @NonNull RuleRunner.ShownSwap shown) {
        return new SwapStore.Request(actionId, shown, "kit history", "kit part history",
                "pantry history", "pantry part history");
    }

    @NonNull
    private SwapStore.Result confirm(@NonNull SwapStore.Request request,
                                     @NonNull RotationPlannerContract planner,
                                     @NonNull LocalDate today) {
        return store.confirm(request, planner, () -> today, () -> SETTINGS);
    }

    @NonNull
    private BatchInfo info(long batchId) {
        BatchInfo batch = db.batchDao().getInfo(batchId);
        assertNotNull(batch);
        return batch;
    }

    private void assertBatch(long batchId, long locationId, int quantity, @NonNull LocalDate date) {
        BatchInfo batch = db.batchDao().getInfo(batchId);
        assertNotNull(batch);
        assertEquals(locationId, batch.getLocationId());
        assertEquals(quantity, batch.getQuantity());
        assertEquals(date, batch.getDate());
    }

    /** The id of the one batch at this location with this date. */
    private long onlyBatchAt(long locationId, @NonNull LocalDate date) {
        List<Long> ids = new ArrayList<>();
        for (BatchInfo batch : db.batchDao().getActiveInfo()) {
            if (batch.getLocationId() == locationId && date.equals(batch.getDate())) {
                ids.add(batch.getId());
            }
        }
        assertEquals(1, ids.size());
        assertNotEquals(0L, (long) ids.get(0));
        return ids.get(0);
    }

    @NonNull
    private static List<String> asList(@NonNull String... rows) {
        List<String> list = new ArrayList<>();
        for (String row : rows) {
            list.add(row);
        }
        return list;
    }
}

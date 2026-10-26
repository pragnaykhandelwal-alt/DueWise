package com.example.readyshelf.data;

import static com.example.readyshelf.data.StoreTestData.count;
import static com.example.readyshelf.data.StoreTestData.dump;
import static com.example.readyshelf.data.StoreTestData.location;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.data.ReadyShelfRepository.SaveListener;
import com.example.readyshelf.data.ReadyShelfRepository.SaveResult;
import com.example.readyshelf.domain.ConditionAnswer;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.FoodClass;
import com.example.readyshelf.domain.FoodType;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Storage tests for ReadyShelfRepository: which product a new batch gets, what an edit keeps,
 * removing the sample items that an earlier version added to the household's records, what
 * happens when the database reports an error while a location is added or those sample items are
 * removed, and (since 2026-10-24) "Renewed" and "Discard" on the Purchase/Renewal list.
 * Each test uses a database in memory with made-up rows.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReadyShelfRepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AppDatabase db;
    private ReadyShelfRepository repository;
    private long pantryId;

    @Before
    public void createDatabase() {
        db = StoreTestData.inMemory(context);
        repository = new ReadyShelfRepository(db);
        pantryId = location(db, "Pantry", LocationType.PANTRY);
    }

    @After
    public void closeDatabase() {
        db.close();
    }

    // ----- Which product a new batch gets -----

    @Test
    public void aProductTypedWithOtherCapitalLetters_isTheSameProduct() throws Exception {
        assertEquals(SaveResult.SAVED, addBatch(product("Canned beans", "Low sodium", "15 oz", "cans")));
        assertEquals(SaveResult.SAVED, addBatch(product("CANNED BEANS", "low SODIUM", "15 OZ", "Cans")));

        assertEquals(1, count(db, "products"));
        assertEquals(2, count(db, "batches"));
    }

    @Test
    public void anotherVariantPackageSizeOrUnit_isANewProduct() throws Exception {
        addBatch(product("Canned beans", "Low sodium", "15 oz", "cans"));
        addBatch(product("Canned beans", "", "15 oz", "cans"));           // no variant
        addBatch(product("Canned beans", "Low sodium", "29 oz", "cans"));  // another size
        addBatch(product("Canned beans", "Low sodium", "15 oz", "trays")); // another unit

        assertEquals(4, count(db, "products"));
    }

    @Test
    public void aSampleProductIsNeverUsedForTheHouseholdsBatch() throws Exception {
        long sampleProduct = StoreTestData.product(db, "Granola bars", "bars", null, true);

        assertEquals(SaveResult.SAVED, addBatch(product("Granola bars", "", "", "bars")));

        assertEquals(2, count(db, "products"));
        Batch added = db.batchDao().getById(onlyBatchId());
        assertNotNull(added);
        assertNotEquals(sampleProduct, added.productId);
    }

    // ----- What an edit keeps -----

    // Since 2026-10-22 the form has no food section. An edit must still keep the food type, the
    // condition answers, and the product's food class that an older version saved.

    @Test
    public void anEdit_keepsTheFoodTypeTheAnswers_andTheProductsFoodClass() throws Exception {
        long soupId = StoreTestData.product(db, "Test soup", "cans", FoodClass.PERISHABLE_FOOD, false);
        long batchId = olderFoodBatch(soupId);

        assertEquals(SaveResult.SAVED, await(listener -> repository.updateBatch(null,
                edited(batchId, soupId, ItemCategory.FOOD, 3), 2, "Count corrected.", listener)));

        Batch saved = db.batchDao().getById(batchId);
        assertNotNull(saved);
        assertEquals(3, saved.quantity);
        assertOlderFoodValues(saved);
        assertEquals(FoodClass.PERISHABLE_FOOD, db.productDao().getById(soupId).foodClass);
    }

    @Test
    public void anEditToAnotherCategory_keepsTheStoredFoodValuesToo() throws Exception {
        long soupId = StoreTestData.product(db, "Test soup", "cans", FoodClass.PERISHABLE_FOOD, false);
        long batchId = olderFoodBatch(soupId);

        assertEquals(SaveResult.SAVED, await(listener -> repository.updateBatch(null,
                edited(batchId, soupId, ItemCategory.WATER, 2), 2, "Category changed.", listener)));

        Batch saved = db.batchDao().getById(batchId);
        assertNotNull(saved);
        assertEquals(ItemCategory.WATER, saved.category);
        assertOlderFoodValues(saved);
        assertEquals(FoodClass.PERISHABLE_FOOD, db.productDao().getById(soupId).foodClass);
    }

    // ----- A database error changes nothing, and the screen is told -----

    // Opening the separate sample data when the database fails: DataSetsTest.

    @Test
    public void removeSampleData_whenTheDatabaseFails_changesNothing() throws Exception {
        addOldSampleBars();
        failEvery("DELETE ON batches");
        List<String> before = dump(db);

        assertEquals(SaveResult.FAILED, await(repository::removeSampleData));

        assertEquals(before, dump(db));
    }

    // ----- Sample items that an earlier version added to the household's records -----

    @Test
    public void removeSampleData_removesOnlyTheSampleItems() throws Exception {
        assertEquals(SaveResult.SAVED, addBatch(product("Canned beans", "", "15 oz", "cans")));
        addOldSampleBars();

        assertEquals(SaveResult.SAVED, await(repository::removeSampleData));

        assertEquals(Collections.singletonList("Canned beans|0"), StoreTestData.rows(
                db.getOpenHelper().getReadableDatabase(), "SELECT name, sample FROM products"));
        assertEquals(Collections.singletonList("1|0"), StoreTestData.rows(
                db.getOpenHelper().getReadableDatabase(), "SELECT quantity, sample FROM batches"));
    }

    @Test
    public void addLocation_whenTheDatabaseFails_changesNothing() throws Exception {
        failEvery("INSERT ON locations");
        List<String> before = dump(db);

        assertEquals(SaveResult.FAILED, await(listener ->
                repository.addLocation("Car kit", LocationType.EMERGENCY_KIT, listener)));

        assertEquals(before, dump(db));
    }

    @Test
    public void addLocation_withATakenName_addsNothing() throws Exception {
        assertEquals(SaveResult.SAVED, await(listener ->
                repository.addLocation("PANTRY", LocationType.OTHER, listener)));

        assertEquals(1, count(db, "locations"));
    }

    // ----- "Renewed" and "Discard" on the Purchase/Renewal list (2026-10-24) -----

    @Test
    public void renewBatch_savesTheNewDateAsConfirmed_withOneHistoryEntry() throws Exception {
        // Not confirmed at the start, so the test shows that the new date is saved as confirmed.
        long passportId = passport(DAY.minusDays(1), false);

        assertEquals(SaveResult.SAVED, await(listener -> repository.renewBatch(passportId, 1,
                DAY.minusDays(1), DAY.plusYears(10), "Renewed.", listener)));

        Batch saved = db.batchDao().getById(passportId);
        assertNotNull(saved);
        assertEquals(DAY.plusYears(10), saved.date);
        assertNull(saved.printedDateText);
        assertTrue(saved.dateConfirmed);
        assertEquals(DateLabel.EXPIRES, saved.dateLabel);
        assertEquals(1, saved.quantity);
        assertEquals(pantryId, saved.locationId);
        assertFalse(saved.archived);
        assertEquals(Collections.singletonList("EDITED|Renewed."), history(passportId));
    }

    @Test
    public void renewBatch_whenTheBatchChanged_savesNothing() throws Exception {
        long passportId = passport(DAY.minusDays(1), true);
        List<String> before = dump(db);

        // Another date, another quantity, or no batch at all since the screen showed it.
        assertEquals(SaveResult.BATCH_CHANGED, await(listener -> repository.renewBatch(
                passportId, 1, DAY.minusDays(2), DAY.plusYears(10), "Renewed.", listener)));
        assertEquals(SaveResult.BATCH_CHANGED, await(listener -> repository.renewBatch(
                passportId, 2, DAY.minusDays(1), DAY.plusYears(10), "Renewed.", listener)));
        assertEquals(SaveResult.BATCH_CHANGED, await(listener -> repository.renewBatch(
                passportId + 1, 1, DAY.minusDays(1), DAY.plusYears(10), "Renewed.", listener)));

        assertEquals(before, dump(db));
    }

    @Test
    public void renewBatch_ofAnArchivedBatch_savesNothing() throws Exception {
        long passportId = passport(DAY.minusDays(1), true);
        assertEquals(SaveResult.SAVED, await(listener ->
                repository.archiveBatch(passportId, 1, "Archived.", listener)));
        List<String> before = dump(db);

        assertEquals(SaveResult.BATCH_CHANGED, await(listener -> repository.renewBatch(
                passportId, 1, DAY.minusDays(1), DAY.plusYears(10), "Renewed.", listener)));

        assertEquals(before, dump(db));
    }

    @Test
    public void renewBatch_whenTheDatabaseFails_changesNothing() throws Exception {
        long passportId = passport(DAY.minusDays(1), true);
        // The batch is saved first; then its history entry fails, so the new date must go too.
        failEvery("INSERT ON batch_events");
        List<String> before = dump(db);

        assertEquals(SaveResult.FAILED, await(listener -> repository.renewBatch(passportId, 1,
                DAY.minusDays(1), DAY.plusYears(10), "Renewed.", listener)));

        assertEquals(before, dump(db));
    }

    @Test
    public void discardAndArchive_recordsTheDiscard_thenArchives() throws Exception {
        long passportId = passport(DAY.minusDays(1), true);

        assertEquals(SaveResult.SAVED, await(listener -> repository.discardAndArchive(
                passportId, 1, "Discarded.", "Archived.", listener)));

        Batch saved = db.batchDao().getById(passportId);
        assertNotNull(saved);
        assertEquals(0, saved.quantity);
        assertTrue(saved.archived);
        assertEquals(DAY.minusDays(1), saved.date);
        assertEquals(Arrays.asList("DISCARDED|Discarded.", "ARCHIVED|Archived."),
                history(passportId));
        assertTrue(db.batchDao().getActiveInfo().isEmpty());
    }

    @Test
    public void discardAndArchive_whenTheBatchChanged_savesNothing() throws Exception {
        long passportId = passport(DAY.minusDays(1), true);
        List<String> before = dump(db);

        // Another quantity, or no batch at all since the screen showed it.
        assertEquals(SaveResult.BATCH_CHANGED, await(listener -> repository.discardAndArchive(
                passportId, 2, "Discarded.", "Archived.", listener)));
        assertEquals(SaveResult.BATCH_CHANGED, await(listener -> repository.discardAndArchive(
                passportId + 1, 1, "Discarded.", "Archived.", listener)));

        assertEquals(before, dump(db));
    }

    @Test
    public void discardAndArchive_whenTheDatabaseFails_changesNothing() throws Exception {
        long passportId = passport(DAY.minusDays(1), true);
        failEvery("INSERT ON batch_events");
        List<String> before = dump(db);

        assertEquals(SaveResult.FAILED, await(listener -> repository.discardAndArchive(
                passportId, 1, "Discarded.", "Archived.", listener)));

        assertEquals(before, dump(db));
    }

    // ----- Helpers -----

    /** A Passport: 1 document in the pantry with an "Expires" date, as the form saves one. */
    private long passport(@NonNull LocalDate date, boolean confirmed) {
        Batch batch = new Batch();
        batch.productId = StoreTestData.product(db, "Passport", "document", null, false);
        batch.locationId = pantryId;
        batch.category = ItemCategory.DOCUMENT;
        batch.quantity = 1;
        batch.date = date;
        batch.dateLabel = DateLabel.EXPIRES;
        batch.dateConfirmed = confirmed;
        return db.batchDao().insert(batch);
    }

    /** One batch's history, oldest first, as "TYPE|details". */
    @NonNull
    private List<String> history(long batchId) {
        return StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT type, details FROM batch_events WHERE batchId = " + batchId + " ORDER BY id");
    }

    @NonNull
    private static Product product(@NonNull String name, @NonNull String variant,
                                   @NonNull String packageSize, @NonNull String unit) {
        Product product = new Product();
        product.name = name;
        product.variant = variant;
        product.packageSize = packageSize;
        product.unit = unit;
        return product;
    }

    /** A sample product and batch, as "Load sample data" added them before 2026-10-12. */
    private void addOldSampleBars() {
        long sampleBars = StoreTestData.product(db, "Granola bars", "bars", null, true);
        StoreTestData.foodBatch(db, sampleBars, pantryId, 6, DAY.plusDays(5), true);
    }

    /** Adds one food batch of a new product to the pantry, and waits for the result. */
    @NonNull
    private SaveResult addBatch(@NonNull Product product) throws Exception {
        Batch batch = new Batch();
        batch.category = ItemCategory.FOOD;
        batch.quantity = 1;
        batch.dateLabel = DateLabel.BEST_BY;
        batch.locationId = pantryId;
        return await(listener -> repository.addBatch(product, batch, "Added.", listener));
    }

    /**
     * A pantry batch of 2 cans as an older version saved it: Canned, Sealed No, Shelf-stable Yes,
     * Damaged Yes, and no storage-concern answer.
     */
    private long olderFoodBatch(long productId) {
        Batch batch = edited(0, productId, ItemCategory.FOOD, 2);
        batch.foodType = FoodType.CANNED;
        batch.sealed = ConditionAnswer.NO;
        batch.shelfStable = ConditionAnswer.YES;
        batch.damaged = ConditionAnswer.YES;
        return db.batchDao().insert(batch);
    }

    /** What the form sends for an edit: only its own values, so no food type or answers. */
    @NonNull
    private Batch edited(long batchId, long productId, @NonNull ItemCategory category, int quantity) {
        Batch batch = new Batch();
        batch.id = batchId;
        batch.productId = productId;
        batch.locationId = pantryId;
        batch.category = category;
        batch.quantity = quantity;
        batch.date = DAY.plusDays(60);
        batch.dateLabel = DateLabel.BEST_BY;
        batch.dateConfirmed = true;
        return batch;
    }

    private static void assertOlderFoodValues(@NonNull Batch saved) {
        assertEquals(FoodType.CANNED, saved.foodType);
        assertEquals(ConditionAnswer.NO, saved.sealed);
        assertEquals(ConditionAnswer.YES, saved.shelfStable);
        assertEquals(ConditionAnswer.YES, saved.damaged);
        assertNull(saved.storageConcern);
    }

    private long onlyBatchId() {
        List<String> ids = StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT id FROM batches");
        assertEquals(1, ids.size());
        return Long.parseLong(ids.get(0));
    }

    /**
     * A test-only table rule that makes every such change fail, as if the phone's storage had
     * a problem at that moment.
     */
    private void failEvery(@NonNull String change) {
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER made_up_failure BEFORE "
                + change + " BEGIN SELECT RAISE(ABORT, 'made-up failure'); END");
    }

    private interface Change {
        void start(@NonNull SaveListener listener);
    }

    /** Starts a change on the repository's background thread and waits for its result. */
    @NonNull
    private static SaveResult await(@NonNull Change change) throws Exception {
        BlockingQueue<SaveResult> results = new ArrayBlockingQueue<>(1);
        change.start(results::add);
        SaveResult result = results.poll(10, TimeUnit.SECONDS);
        assertNotNull("No result within 10 seconds", result);
        return result;
    }
}

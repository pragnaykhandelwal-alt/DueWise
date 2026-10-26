package com.example.readyshelf.data;

import static com.example.readyshelf.data.StoreTestData.count;
import static com.example.readyshelf.data.StoreTestData.dump;
import static com.example.readyshelf.data.StoreTestData.rows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.data.ReadyShelfRepository.SaveResult;
import com.example.readyshelf.domain.FoodClass;
import com.example.readyshelf.domain.LocationType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * "Keep these items as my own": every batch in the sample data is copied into the household's
 * records exactly as it is, but no longer marked sample, with its history and one more entry.
 * Places and products the household has are used, not added twice, and nothing the household
 * had changes. A failure copies nothing, only sample data can be copied (and only into the
 * household's file), and after the copy the sample file is empty, so nothing is copied twice.
 *
 * <p>Every test uses files of its own, deleted before and after it. They never touch the app's
 * own database or settings files. The rows are made up.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class KeepSampleDataTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 18);
    private static final String HOUSEHOLD_FILE = "keep-test-household.db";
    private static final String HOUSEHOLD_SETTINGS = "keep-test-settings";
    private static final String SAMPLE_FILE = "keep-test-sample-data.db";
    private static final String SAMPLE_SETTINGS = "keep-test-sample-settings";
    private static final String MODE_FILE = "keep-test-mode";
    private static final String MADE_UP = "Sample data: a made-up item for trying the app.";
    private static final String COPIED = "Copied from the sample data into your own records.";
    private static final String[] TABLES = {"locations", "products", "batches", "batch_events",
            "swap_actions", "purchase_items"};
    /** Each batch as its product, place, and every value, so a copy can be compared with it. */
    private static final String BATCH_ROWS = "SELECT p.name, p.variant, p.packageSize, p.unit, "
            + "l.name, l.type, b.category, b.quantity, b.date, b.printedDateText, b.dateLabel, "
            + "b.dateConfirmed, b.foodType, b.sealed, b.shelfStable, b.damaged, b.storageConcern, "
            + "b.archived, b.sample FROM batches AS b JOIN products AS p ON p.id = b.productId "
            + "JOIN locations AS l ON l.id = b.locationId";

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final List<DataSets> made = new ArrayList<>();
    private DataSets sets;

    @Before
    public void startWithoutTestFiles() {
        deleteTestFiles();
        sets = newDataSets();
    }

    @After
    public void deleteTheTestFiles() {
        for (DataSets each : made) {
            each.close();
        }
        deleteTestFiles();
    }

    @Test
    public void everyBatch_isCopiedAsItIs_butNoLongerMarkedSample() throws Exception {
        addHouseholdRice();
        open(DAY);
        List<String> sampleBatches = rows(sql(sets.sample().database), BATCH_ROWS + " ORDER BY b.id");

        assertEquals(new Copy(SaveResult.SAVED, 5), keep(sets));

        // The same values, only the sample mark (the last column) is gone.
        List<String> expected = new ArrayList<>();
        for (String row : sampleBatches) {
            assertTrue(row, row.endsWith("|1"));
            expected.add(row.substring(0, row.length() - 1) + "0");
        }
        assertEquals(expected, rows(sql(sets.household().database),
                BATCH_ROWS + " WHERE p.name <> 'Rice' ORDER BY b.id"));
        assertEquals(Arrays.asList("Rice|bags|SHELF_STABLE_FOOD|0",
                "Granola bars|bars|SHELF_STABLE_FOOD|0", "Canned beans|cans|SHELF_STABLE_FOOD|0",
                "Pain reliever tablets|bottles|null|0"), rows(sql(sets.household().database),
                "SELECT name, unit, foodClass, sample FROM products ORDER BY id"));
        // The household's three starting places are used, not added twice.
        assertEquals(Arrays.asList("Pantry|PANTRY", "Emergency kit|EMERGENCY_KIT",
                "First aid box|FIRST_AID"), rows(sql(sets.household().database),
                "SELECT name, type FROM locations ORDER BY id"));
    }

    @Test
    public void eachCopy_keepsItsHistory_andGetsOneMoreEntry() throws Exception {
        open(DAY);
        List<String> sampleHistory = rows(sql(sets.sample().database),
                "SELECT type, details, occurredAtMillis FROM batch_events ORDER BY batchId, id");

        assertEquals(new Copy(SaveResult.SAVED, 5), keep(sets));

        SupportSQLiteDatabase household = sql(sets.household().database);
        assertEquals(sampleHistory, rows(household, "SELECT type, details, occurredAtMillis "
                + "FROM batch_events WHERE details = '" + MADE_UP + "' ORDER BY batchId, id"));
        assertEquals(Collections.nCopies(5, "ADDED|" + COPIED), rows(household,
                "SELECT type, details FROM batch_events WHERE details <> '" + MADE_UP
                        + "' ORDER BY batchId, id"));
        assertEquals(Collections.nCopies(5, "2"), rows(household,
                "SELECT COUNT(*) FROM batch_events GROUP BY batchId ORDER BY batchId"));
    }

    @Test
    public void placesAndProductsTheHouseholdHas_areUsed_andOthersAreAdded() throws Exception {
        AppDatabase household = sets.household().database;
        // The household's own beans, written differently, and not classified: they stay so.
        Product beans = new Product();
        beans.name = "CANNED BEANS";
        beans.variant = "black beans";
        beans.packageSize = "15 OZ";
        beans.unit = "Cans";
        long householdBeans = household.productDao().insert(beans);
        StoreTestData.location(household, "car kit", LocationType.EMERGENCY_KIT);
        List<String> householdBefore = dump(household);
        open(DAY);
        AppDatabase sample = sets.sample().database;
        // While sample data is open: two new places, and a batch added by hand in one of them.
        long carKit = StoreTestData.location(sample, "Car kit", LocationType.EMERGENCY_KIT);
        StoreTestData.location(sample, "Garage", LocationType.OTHER);
        long water = StoreTestData.product(sample, "Water", "bottles", null, false);
        StoreTestData.foodBatch(sample, water, carKit, 12, DAY.plusDays(400), false);

        assertEquals(new Copy(SaveResult.SAVED, 6), keep(sets));

        SupportSQLiteDatabase sql = sql(household);
        assertEquals(Arrays.asList("Pantry|PANTRY", "Emergency kit|EMERGENCY_KIT",
                "First aid box|FIRST_AID", "car kit|EMERGENCY_KIT", "Garage|OTHER"),
                rows(sql, "SELECT name, type FROM locations ORDER BY id"));
        assertEquals(Arrays.asList("CANNED BEANS|null|0", "Granola bars|SHELF_STABLE_FOOD|0",
                "Pain reliever tablets|null|0", "Water|null|0"),
                rows(sql, "SELECT name, foodClass, sample FROM products ORDER BY id"));
        assertEquals(Collections.singletonList(String.valueOf(householdBeans)), rows(sql,
                "SELECT productId FROM batches WHERE quantity = 4"));
        assertEquals(Collections.singletonList("car kit|12"), rows(sql, "SELECT l.name, "
                + "b.quantity FROM batches AS b JOIN locations AS l ON l.id = b.locationId "
                + "JOIN products AS p ON p.id = b.productId WHERE p.name = 'Water'"));
        // Everything the household had is still there, unchanged.
        assertTrue(dump(household).containsAll(householdBefore));
    }

    @Test
    public void thePurchaseList_isCopied_butTheHouseholdsOwnEntryIsKept() throws Exception {
        AppDatabase household = sets.household().database;
        Product bars = new Product();
        bars.name = "Granola bars";
        bars.variant = "Oat and honey";
        bars.packageSize = "1.5 oz";
        bars.unit = "bars";
        long householdBars = household.productDao().insert(bars);
        long householdKit = kitId(household);
        purchaseItem(household, householdBars, householdKit, 2);
        open(DAY);
        AppDatabase sample = sets.sample().database;
        long sampleKit = kitId(sample);
        purchaseItem(sample, productId(sample, "Granola bars"), sampleKit, 6);
        purchaseItem(sample, productId(sample, "Canned beans"), sampleKit, 4);

        assertEquals(new Copy(SaveResult.SAVED, 5), keep(sets));

        assertEquals(Arrays.asList("Granola bars|Emergency kit|2", "Canned beans|Emergency kit|4"),
                rows(sql(household), "SELECT p.name, l.name, i.quantity FROM purchase_items AS i "
                        + "JOIN products AS p ON p.id = i.productId "
                        + "JOIN locations AS l ON l.id = i.kitLocationId ORDER BY i.id"));
    }

    @Test
    public void aFailedCopy_changesNothing_andSampleDataStaysOpen() throws Exception {
        addHouseholdRice();
        open(DAY);
        List<String> householdBefore = dump(sets.household().database);
        List<String> sampleBefore = dump(sets.sample().database);
        // The batches would be added, but their history can't be, so everything is undone.
        sets.household().database.getOpenHelper().getWritableDatabase().execSQL(
                "CREATE TRIGGER made_up_failure BEFORE INSERT ON batch_events "
                        + "BEGIN SELECT RAISE(ABORT, 'made-up failure'); END");

        assertEquals(new Copy(SaveResult.FAILED, 0), keep(sets));

        assertEquals(householdBefore, dump(sets.household().database));
        assertEquals(sampleBefore, dump(sets.sample().database));
        assertTrue(sets.isSampleOpen());
    }

    @Test
    public void onlySampleData_isCopied_andOnlyIntoTheHouseholdsFile() throws Exception {
        addHouseholdRice();
        open(DAY);
        ReadyShelfRepository household = sets.household().repository;
        ReadyShelfRepository sample = sets.sample().repository;
        List<String> householdBefore = dump(sets.household().database);
        List<String> sampleBefore = dump(sets.sample().database);

        assertEquals(new Copy(SaveResult.FAILED, 0),
                await(listener -> sample.copyFromSampleData(household, COPIED, listener)));
        assertEquals(new Copy(SaveResult.FAILED, 0),
                await(listener -> household.copyFromSampleData(household, COPIED, listener)));
        assertEquals(new Copy(SaveResult.FAILED, 0),
                await(listener -> sample.copyFromSampleData(sample, COPIED, listener)));

        assertEquals(householdBefore, dump(sets.household().database));
        assertEquals(sampleBefore, dump(sets.sample().database));
    }

    @Test
    public void afterTheCopy_theSampleFileIsEmpty_soNothingIsCopiedTwice() throws Exception {
        addHouseholdRice();
        open(DAY);

        assertEquals(new Copy(SaveResult.SAVED, 5), keep(sets));
        for (String table : TABLES) {
            assertEquals(table, 0, count(sets.sample().database, table));
        }
        assertEquals(new Copy(SaveResult.SAVED, 0), keep(sets));
        assertEquals(6, count(sets.household().database, "batches"));

        // Step 2, as Settings did until 2026-10-22: sample data closes, and the household's
        // records are shown.
        BlockingQueue<SaveResult> emptied = new ArrayBlockingQueue<>(1);
        assertTrue(askOnMain(() -> sets.switchToHousehold(emptied::add)));
        assertEquals(SaveResult.SAVED, emptied.poll(10, TimeUnit.SECONDS));
        assertFalse(sets.isSampleOpen());
        assertSame(sets.household(), sets.active());
        assertEquals(6, count(sets.household().database, "batches"));
        assertEquals(Collections.singletonList("0"), rows(sql(sets.household().database),
                "SELECT COUNT(*) FROM batches WHERE sample = 1"));
    }

    // ----- Helpers -----

    /** What a copy reported: its result and how many batches it copied. */
    private static final class Copy {
        final SaveResult result;
        final int batches;

        Copy(@NonNull SaveResult result, int batches) {
            this.result = result;
            this.batches = batches;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Copy && ((Copy) other).result == result
                    && ((Copy) other).batches == batches;
        }

        @Override
        public int hashCode() {
            return result.hashCode() * 31 + batches;
        }

        @NonNull
        @Override
        public String toString() {
            return result + ", " + batches + " batches";
        }
    }

    @NonNull
    private DataSets newDataSets() {
        DataSets dataSets = new DataSets(context, HOUSEHOLD_FILE, HOUSEHOLD_SETTINGS, SAMPLE_FILE,
                SAMPLE_SETTINGS, MODE_FILE);
        made.add(dataSets);
        return dataSets;
    }

    private void deleteTestFiles() {
        context.deleteDatabase(HOUSEHOLD_FILE);
        context.deleteDatabase(SAMPLE_FILE);
        context.deleteSharedPreferences(HOUSEHOLD_SETTINGS);
        context.deleteSharedPreferences(SAMPLE_SETTINGS);
        context.deleteSharedPreferences(MODE_FILE);
    }

    /**
     * Opens sample data the way Settings did until 2026-10-22: step 1 in the background, step 2
     * on the main thread.
     */
    private void open(@NonNull LocalDate today) throws Exception {
        BlockingQueue<SaveResult> results = new ArrayBlockingQueue<>(1);
        sets.prepareSample(today, MADE_UP, results::add);
        assertEquals(SaveResult.SAVED, results.poll(10, TimeUnit.SECONDS));
        assertTrue(askOnMain(sets::switchToSample));
    }

    /** Step 1 of "Keep these items as my own", and waits for its result. */
    @NonNull
    private static Copy keep(@NonNull DataSets dataSets) throws Exception {
        return await(listener -> dataSets.keepSampleAsHousehold(COPIED, listener));
    }

    /** One batch of the household's own, so a test can see that it stays as it was. */
    private void addHouseholdRice() {
        AppDatabase household = sets.household().database;
        long rice = StoreTestData.product(household, "Rice", "bags", FoodClass.SHELF_STABLE_FOOD,
                false);
        StoreTestData.foodBatch(household, rice, pantryId(household), 2, DAY.plusDays(300), false);
    }

    private static long pantryId(@NonNull AppDatabase db) {
        StorageLocation pantry = db.locationDao().findFirstOfType(LocationType.PANTRY);
        assertNotNull(pantry);
        return pantry.id;
    }

    private static long kitId(@NonNull AppDatabase db) {
        StorageLocation kit = db.locationDao().findFirstOfType(LocationType.EMERGENCY_KIT);
        assertNotNull(kit);
        return kit.id;
    }

    private static long productId(@NonNull AppDatabase db, @NonNull String name) {
        List<String> ids = rows(sql(db), "SELECT id FROM products WHERE name = '" + name + "'");
        assertEquals(name, 1, ids.size());
        return Long.parseLong(ids.get(0));
    }

    private static void purchaseItem(@NonNull AppDatabase db, long productId, long kitId,
                                     int quantity) {
        PurchaseItem item = new PurchaseItem();
        item.productId = productId;
        item.kitLocationId = kitId;
        item.quantity = quantity;
        item.addedAtMillis = 1_000L;
        db.purchaseItemDao().insert(item);
    }

    @NonNull
    private static SupportSQLiteDatabase sql(@NonNull AppDatabase db) {
        return db.getOpenHelper().getReadableDatabase();
    }

    private static boolean askOnMain(@NonNull BooleanSupplier question) {
        AtomicBoolean answer = new AtomicBoolean();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> answer.set(question.getAsBoolean()));
        return answer.get();
    }

    private interface CopyChange {
        void start(@NonNull ReadyShelfRepository.CopyListener listener);
    }

    /** Starts a copy on a background thread and waits for its result. */
    @NonNull
    private static Copy await(@NonNull CopyChange change) throws Exception {
        BlockingQueue<Copy> results = new ArrayBlockingQueue<>(1);
        change.start((result, batches) -> results.add(new Copy(result, batches)));
        Copy copy = results.poll(10, TimeUnit.SECONDS);
        assertNotNull("No result within 10 seconds", copy);
        return copy;
    }
}

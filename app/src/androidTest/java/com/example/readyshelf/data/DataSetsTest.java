package com.example.readyshelf.data;

import static com.example.readyshelf.data.StoreTestData.count;
import static com.example.readyshelf.data.StoreTestData.dump;
import static com.example.readyshelf.data.StoreTestData.rows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.data.ReadyShelfRepository.SaveListener;
import com.example.readyshelf.data.ReadyShelfRepository.SaveResult;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.FoodClass;
import com.example.readyshelf.domain.LocationType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * The separate sample data: opening it fills only the sample-data file, the household's records
 * stay exactly as they were, the choice is kept on disk, opening again starts over, closing
 * empties the sample file, a failure switches nothing, and only a sample-data file can ever be
 * emptied.
 *
 * <p>Every test uses files of its own, deleted before and after it. They never touch the app's
 * own database or settings files. The rows are made up.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class DataSetsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 12);
    private static final String HOUSEHOLD_FILE = "datasets-test-household.db";
    private static final String HOUSEHOLD_SETTINGS = "datasets-test-settings";
    private static final String SAMPLE_FILE = "datasets-test-sample-data.db";
    private static final String SAMPLE_SETTINGS = "datasets-test-sample-settings";
    private static final String MODE_FILE = "datasets-test-mode";
    private static final String HISTORY = "Sample data: a made-up item for trying the app.";
    private static final String[] TABLES = {"locations", "products", "batches", "batch_events",
            "swap_actions", "purchase_items"};

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

    // ----- Opening sample data -----

    @Test
    public void opening_fillsOnlyTheSampleFile_andTheHouseholdStaysAsItWas() throws Exception {
        addHouseholdRice();
        onMain(() -> sets.household().settings.saveThresholds(DateThresholds.of(45, 200)));
        List<String> householdBefore = dump(sets.household().database);

        open(DAY);

        assertTrue(sets.isSampleOpen());
        assertSame(sets.sample(), sets.active());
        assertEquals(householdBefore, dump(sets.household().database));
        assertEquals(DateThresholds.of(45, 200), sets.household().settings.current());
        assertEquals(DateThresholds.defaults(), sets.sample().settings.current());

        SupportSQLiteDatabase sample = sql(sets.sample().database);
        assertEquals(Arrays.asList("Pantry|PANTRY", "Emergency kit|EMERGENCY_KIT",
                "First aid box|FIRST_AID"), rows(sample, "SELECT name, type FROM locations ORDER BY id"));
        assertEquals(Arrays.asList("Granola bars|bars|SHELF_STABLE_FOOD|1",
                "Canned beans|cans|SHELF_STABLE_FOOD|1", "Pain reliever tablets|bottles|null|1"),
                rows(sample, "SELECT name, unit, foodClass, sample FROM products ORDER BY id"));
        assertEquals(Arrays.asList(
                "Granola bars|Emergency kit|6|2026-11-01|null|BEST_BY|1|YES|NO|NO|1",
                "Granola bars|Pantry|6|2027-06-09|null|BEST_BY|1|YES|NO|NO|1",
                "Granola bars|Pantry|3|null|07/2027|BEST_BY|0|YES|NO|NO|1",
                "Canned beans|Emergency kit|4|2026-10-26|null|BEST_BY|1|YES|NO|NO|1",
                "Pain reliever tablets|First aid box|2|2026-10-02|null|EXPIRES|1|null|null|null|1"),
                rows(sample, "SELECT p.name, l.name, b.quantity, b.date, b.printedDateText, "
                        + "b.dateLabel, b.dateConfirmed, b.sealed, b.damaged, b.storageConcern, "
                        + "b.sample FROM batches AS b JOIN products AS p ON p.id = b.productId "
                        + "JOIN locations AS l ON l.id = b.locationId ORDER BY b.id"));
        assertEquals(Collections.singletonList("1"), rows(sample, "SELECT COUNT(DISTINCT b.productId) "
                + "FROM batches AS b JOIN products AS p ON p.id = b.productId "
                + "WHERE p.name = 'Granola bars'"));
        assertEquals(Collections.singletonList("5"), rows(sample, "SELECT COUNT(*) FROM batch_events "
                + "WHERE type = 'ADDED' AND details = '" + HISTORY + "'"));
        assertEquals(5, count(sets.sample().database, "batch_events"));
    }

    @Test
    public void theChoice_isOnDisk_andTheNextStartUsesIt() throws Exception {
        open(DAY);
        assertTrue(modeFile(), modeFile().contains("name=\"sample_data_open\" value=\"true\""));

        sets.close();
        DataSets nextStart = newDataSets();
        assertTrue(nextStart.isSampleOpen());
        assertTrue(nextStart.active().sample);

        assertEquals(SaveResult.SAVED, closeAndWait(nextStart));
        assertTrue(modeFile(), modeFile().contains("name=\"sample_data_open\" value=\"false\""));
        nextStart.close();
        DataSets thirdStart = newDataSets();
        assertFalse(thirdStart.isSampleOpen());
        assertSame(thirdStart.household(), thirdStart.active());
    }

    @Test
    public void openingAgain_startsOver_withDatesFromTheNewDay() throws Exception {
        open(DAY);
        AppDatabase sample = sets.sample().database;
        // Changes made while sample data is open: a new place and a batch added by hand.
        assertEquals(SaveResult.SAVED, await(listener -> sets.sample().repository
                .addLocation("Car kit", LocationType.EMERGENCY_KIT, listener)));
        long rice = StoreTestData.product(sample, "Rice", "bags", null, false);
        StoreTestData.foodBatch(sample, rice, pantryId(sample), 2, DAY.plusDays(90), false);

        assertEquals(SaveResult.SAVED,
                await(listener -> sets.prepareSample(DAY.plusDays(5), HISTORY, listener)));

        assertEquals(Arrays.asList("Pantry", "Emergency kit", "First aid box"),
                rows(sql(sample), "SELECT name FROM locations ORDER BY id"));
        assertEquals(5, count(sample, "batches"));
        assertEquals(3, count(sample, "products"));
        assertTrue(rows(sql(sample), "SELECT id FROM products WHERE name = 'Rice'").isEmpty());
        // 17 Oct 2026 + 20 days
        assertEquals(Collections.singletonList("2026-11-06"), rows(sql(sample),
                "SELECT b.date FROM batches AS b JOIN locations AS l ON l.id = b.locationId "
                        + "JOIN products AS p ON p.id = b.productId "
                        + "WHERE l.type = 'EMERGENCY_KIT' AND p.name = 'Granola bars'"));
    }

    @Test
    public void aScreenWatchingTheSampleBatches_getsTheNewRowsWhenItStartsOver() throws Exception {
        open(DAY);
        LiveData<List<BatchInfo>> shown = sets.sample().repository.observeBatches();
        BlockingQueue<List<BatchInfo>> values = new LinkedBlockingQueue<>();
        Observer<List<BatchInfo>> screen = values::add;
        onMain(() -> shown.observeForever(screen));
        try {
            waitForKitGranola(values, DAY.plusDays(20));

            assertEquals(SaveResult.SAVED,
                    await(listener -> sets.prepareSample(DAY.plusDays(3), HISTORY, listener)));

            waitForKitGranola(values, DAY.plusDays(23));
        } finally {
            onMain(() -> shown.removeObserver(screen));
        }
    }

    @Test
    public void aFailedOpening_changesNothing_andSwitchesNothing() throws Exception {
        addHouseholdRice();
        AppDatabase sample = sets.sample().database;
        List<String> sampleBefore = dump(sample); // opens the file: the three starting places
        List<String> householdBefore = dump(sets.household().database);
        failEvery(sample, "INSERT ON batches");

        assertEquals(SaveResult.FAILED, await(listener -> sets.prepareSample(DAY, HISTORY, listener)));

        assertEquals(sampleBefore, dump(sample));
        assertEquals(householdBefore, dump(sets.household().database));
        assertFalse(sets.isSampleOpen());
        assertSame(sets.household(), sets.active());
    }

    // ----- Closing sample data -----

    @Test
    public void closing_switchesBack_andEmptiesTheSampleFile() throws Exception {
        addHouseholdRice();
        List<String> householdBefore = dump(sets.household().database);
        open(DAY);

        assertEquals(SaveResult.SAVED, closeAndWait(sets));

        assertFalse(sets.isSampleOpen());
        assertSame(sets.household(), sets.active());
        for (String table : TABLES) {
            assertEquals(table, 0, count(sets.sample().database, table));
        }
        assertEquals(householdBefore, dump(sets.household().database));
    }

    // ----- Only a sample-data file can be emptied -----

    @Test
    public void theHouseholdsFile_isNeverReplacedOrEmptied() throws Exception {
        addHouseholdRice();
        ReadyShelfRepository household = sets.household().repository;
        List<String> before = dump(sets.household().database);

        assertEquals(SaveResult.FAILED,
                await(listener -> household.replaceWithSampleData(DAY, HISTORY, listener)));
        assertEquals(SaveResult.FAILED, await(household::clearSampleData));

        assertEquals(before, dump(sets.household().database));
    }

    @Test
    public void aDatabaseInMemory_isNeverReplacedOrEmptied() throws Exception {
        AppDatabase memory = StoreTestData.inMemory(context);
        try {
            StoreTestData.location(memory, "Pantry", LocationType.PANTRY);
            ReadyShelfRepository repository = new ReadyShelfRepository(memory);
            List<String> before = dump(memory);

            assertEquals(SaveResult.FAILED,
                    await(listener -> repository.replaceWithSampleData(DAY, HISTORY, listener)));
            assertEquals(SaveResult.FAILED, await(repository::clearSampleData));

            assertEquals(before, dump(memory));
        } finally {
            memory.close();
        }
    }

    @Test
    public void aSampleFileNameThatIsNotForSampleData_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> new DataSets(context, HOUSEHOLD_FILE,
                HOUSEHOLD_SETTINGS, "datasets-test-other.db", SAMPLE_SETTINGS, MODE_FILE));
        assertThrows(IllegalArgumentException.class, () -> new DataSets(context, SAMPLE_FILE,
                HOUSEHOLD_SETTINGS, SAMPLE_FILE, SAMPLE_SETTINGS, MODE_FILE));
        assertThrows(IllegalArgumentException.class, () -> new DataSets(context, HOUSEHOLD_FILE,
                HOUSEHOLD_SETTINGS, AppDatabase.FILE_NAME, SAMPLE_SETTINGS, MODE_FILE));
    }

    // ----- Helpers -----

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
        assertEquals(SaveResult.SAVED, await(listener -> sets.prepareSample(today, HISTORY, listener)));
        assertTrue(askOnMain(sets::switchToSample));
    }

    /** Closes sample data, and waits until the sample file was emptied. */
    @NonNull
    private SaveResult closeAndWait(@NonNull DataSets dataSets) throws Exception {
        BlockingQueue<SaveResult> emptied = new ArrayBlockingQueue<>(1);
        assertTrue(askOnMain(() -> dataSets.switchToHousehold(emptied::add)));
        SaveResult result = emptied.poll(10, TimeUnit.SECONDS);
        assertNotNull("The sample file was not emptied within 10 seconds", result);
        return result;
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

    @NonNull
    private static SupportSQLiteDatabase sql(@NonNull AppDatabase db) {
        return db.getOpenHelper().getReadableDatabase();
    }

    /** The mode file as it is on disk. Android also keeps a copy in memory while the app runs. */
    @NonNull
    private String modeFile() throws Exception {
        File file = new File(context.getApplicationInfo().dataDir,
                "shared_prefs/" + MODE_FILE + ".xml");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /**
     * A test-only table rule that makes every such change fail, as if the phone's storage had
     * a problem at that moment.
     */
    private static void failEvery(@NonNull AppDatabase db, @NonNull String change) {
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER made_up_failure BEFORE "
                + change + " BEGIN SELECT RAISE(ABORT, 'made-up failure'); END");
    }

    /** Waits until the screen gets a list in which the kit's granola has {@code date}. */
    private static void waitForKitGranola(@NonNull BlockingQueue<List<BatchInfo>> values,
                                          @NonNull LocalDate date) throws InterruptedException {
        long end = System.currentTimeMillis() + 10_000;
        long left;
        while ((left = end - System.currentTimeMillis()) > 0) {
            List<BatchInfo> list = values.poll(left, TimeUnit.MILLISECONDS);
            if (list != null && date.equals(kitGranolaDate(list))) {
                return;
            }
        }
        fail("No list with the kit's granola dated " + date + " within 10 seconds");
    }

    @Nullable
    private static LocalDate kitGranolaDate(@NonNull List<BatchInfo> batches) {
        for (BatchInfo batch : batches) {
            if (batch.getName().equals("Granola bars")
                    && batch.getLocationType() == LocationType.EMERGENCY_KIT) {
                return batch.getDate();
            }
        }
        return null;
    }

    private static void onMain(@NonNull Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private static boolean askOnMain(@NonNull BooleanSupplier question) {
        AtomicBoolean answer = new AtomicBoolean();
        onMain(() -> answer.set(question.getAsBoolean()));
        return answer.get();
    }

    private interface Change {
        void start(@NonNull SaveListener listener);
    }

    /** Starts a change on a repository's background thread and waits for its result. */
    @NonNull
    private static SaveResult await(@NonNull Change change) throws Exception {
        BlockingQueue<SaveResult> results = new ArrayBlockingQueue<>(1);
        change.start(results::add);
        SaveResult result = results.poll(10, TimeUnit.SECONDS);
        assertNotNull("No result within 10 seconds", result);
        return result;
    }
}

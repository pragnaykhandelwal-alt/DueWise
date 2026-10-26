package com.example.readyshelf.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.FoodClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Checks that the database migrations keep household data.
 *
 * <p>Each test writes an old database file (version 1, 2, or 3) with plain SQLite (made-up rows),
 * then opens it with Room. Room runs the migrations up to version 4 and then checks every table
 * against the version 4 schema; a mismatch stops the test with "Migration didn't properly handle".
 *
 * <p>Run these tests only on the emulator, never on the household phone: Gradle's connected
 * tests uninstall the app afterwards, which deletes its data (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test.db";

    // The version 1 tables, copied from app/schemas/com.example.readyshelf.data.AppDatabase/1.json.
    private static final String[] VERSION_1_SCHEMA = {
            "CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`name` TEXT NOT NULL, `type` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `batches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`name` TEXT NOT NULL, `category` TEXT NOT NULL, `quantity` INTEGER NOT NULL, "
                    + "`unit` TEXT NOT NULL, `date` TEXT, `dateLabel` TEXT NOT NULL, "
                    + "`locationId` INTEGER NOT NULL, `sample` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_batches_locationId` ON `batches` (`locationId`)",
            "CREATE TABLE IF NOT EXISTS `batch_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`batchId` INTEGER NOT NULL, `occurredAtMillis` INTEGER NOT NULL, "
                    + "`type` TEXT NOT NULL, `details` TEXT NOT NULL, "
                    + "FOREIGN KEY(`batchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_batch_events_batchId` ON `batch_events` (`batchId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) "
                    + "VALUES(42, '58f5fb04cc1d87f293f2c5e06fd4b8db')"
    };

    // The version 2 tables, copied from app/schemas/com.example.readyshelf.data.AppDatabase/2.json.
    private static final String[] VERSION_2_SCHEMA = {
            "CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`name` TEXT NOT NULL, `type` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`name` TEXT NOT NULL, `variant` TEXT NOT NULL, `packageSize` TEXT NOT NULL, "
                    + "`unit` TEXT NOT NULL, `sample` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `batches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`productId` INTEGER NOT NULL, `category` TEXT NOT NULL, `quantity` INTEGER NOT NULL, "
                    + "`date` TEXT, `printedDateText` TEXT, `dateLabel` TEXT NOT NULL, "
                    + "`dateConfirmed` INTEGER NOT NULL, `locationId` INTEGER NOT NULL, `foodType` TEXT, "
                    + "`sealed` TEXT, `shelfStable` TEXT, `damaged` TEXT, `storageConcern` TEXT, "
                    + "`archived` INTEGER NOT NULL, `sample` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION , "
                    + "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_batches_locationId` ON `batches` (`locationId`)",
            "CREATE INDEX IF NOT EXISTS `index_batches_productId` ON `batches` (`productId`)",
            "CREATE TABLE IF NOT EXISTS `batch_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`batchId` INTEGER NOT NULL, `occurredAtMillis` INTEGER NOT NULL, "
                    + "`type` TEXT NOT NULL, `details` TEXT NOT NULL, "
                    + "FOREIGN KEY(`batchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_batch_events_batchId` ON `batch_events` (`batchId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) "
                    + "VALUES(42, '9df9cd5ee807bceb5d6d94e780ff8304')"
    };

    // The version 3 tables, copied from app/schemas/com.example.readyshelf.data.AppDatabase/3.json.
    private static final String[] VERSION_3_SCHEMA = {
            "CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`name` TEXT NOT NULL, `type` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`name` TEXT NOT NULL, `variant` TEXT NOT NULL, `packageSize` TEXT NOT NULL, "
                    + "`unit` TEXT NOT NULL, `sample` INTEGER NOT NULL, `foodClass` TEXT)",
            "CREATE TABLE IF NOT EXISTS `batches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`productId` INTEGER NOT NULL, `category` TEXT NOT NULL, `quantity` INTEGER NOT NULL, "
                    + "`date` TEXT, `printedDateText` TEXT, `dateLabel` TEXT NOT NULL, "
                    + "`dateConfirmed` INTEGER NOT NULL, `locationId` INTEGER NOT NULL, `foodType` TEXT, "
                    + "`sealed` TEXT, `shelfStable` TEXT, `damaged` TEXT, `storageConcern` TEXT, "
                    + "`archived` INTEGER NOT NULL, `sample` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION , "
                    + "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_batches_locationId` ON `batches` (`locationId`)",
            "CREATE INDEX IF NOT EXISTS `index_batches_productId` ON `batches` (`productId`)",
            "CREATE TABLE IF NOT EXISTS `batch_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`batchId` INTEGER NOT NULL, `occurredAtMillis` INTEGER NOT NULL, "
                    + "`type` TEXT NOT NULL, `details` TEXT NOT NULL, "
                    + "FOREIGN KEY(`batchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_batch_events_batchId` ON `batch_events` (`batchId`)",
            "CREATE TABLE IF NOT EXISTS `swap_actions` (`actionId` TEXT NOT NULL, "
                    + "`kitBatchId` INTEGER NOT NULL, `pantryBatchId` INTEGER NOT NULL, "
                    + "`quantity` INTEGER NOT NULL, `planDate` TEXT NOT NULL, "
                    + "`appliedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`actionId`), "
                    + "FOREIGN KEY(`kitBatchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE , "
                    + "FOREIGN KEY(`pantryBatchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_swap_actions_kitBatchId` ON `swap_actions` (`kitBatchId`)",
            "CREATE INDEX IF NOT EXISTS `index_swap_actions_pantryBatchId` ON `swap_actions` (`pantryBatchId`)",
            "CREATE TABLE IF NOT EXISTS `purchase_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`productId` INTEGER NOT NULL, `kitLocationId` INTEGER NOT NULL, "
                    + "`quantity` INTEGER NOT NULL, `addedAtMillis` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE , "
                    + "FOREIGN KEY(`kitLocationId`) REFERENCES `locations`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_items_productId_kitLocationId` "
                    + "ON `purchase_items` (`productId`, `kitLocationId`)",
            "CREATE INDEX IF NOT EXISTS `index_purchase_items_kitLocationId` "
                    + "ON `purchase_items` (`kitLocationId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) "
                    + "VALUES(42, '1ce4c134a8a1738d3cf4299f3af1d362')"
    };

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    @After
    public void deleteTestDatabase() {
        context.deleteDatabase(TEST_DB);
    }

    @Test
    public void migrate1To2_keepsEveryBatchAndHistoryEntry() {
        createVersion1(
                "INSERT INTO locations (id, name, type) VALUES "
                        + "(1, 'Pantry', 'PANTRY'), (2, 'Earthquake kit', 'EMERGENCY_KIT')",
                // Two "Granola bars" batches (one product), a name that differs only in capital
                // letters, a batch with no unit, and a sample batch after a gap in the ids.
                "INSERT INTO batches (id, name, category, quantity, unit, date, dateLabel, "
                        + "locationId, sample) VALUES "
                        + "(1, 'Granola bars', 'FOOD', 6, 'bars', '2026-10-12', 'BEST_BY', 2, 0), "
                        + "(2, 'Granola bars', 'FOOD', 10, 'bars', '2027-03-01', 'BEST_BY', 1, 0), "
                        + "(3, 'granola bars', 'FOOD', 2, 'bars', NULL, 'OTHER', 1, 0), "
                        + "(4, 'Bandages', 'FIRST_AID_SUPPLY', 1, '', NULL, 'OTHER', 1, 0), "
                        + "(7, 'Bottled water', 'WATER', 12, 'bottles', '2027-01-01', 'BEST_BY', 2, 1)",
                "INSERT INTO batch_events (id, batchId, occurredAtMillis, type, details) VALUES "
                        + "(1, 1, 1000, 'ADDED', 'Added 6 bars to Earthquake kit.'), "
                        + "(2, 1, 2000, 'EDITED', 'Quantity changed from 8 bars to 6 bars.'), "
                        + "(3, 2, 3000, 'ADDED', 'Added 10 bars to Pantry.'), "
                        + "(4, 3, 3500, 'ADDED', 'Added 2 bars to Pantry.'), "
                        + "(5, 4, 3600, 'ADDED', 'Added 1 to Pantry.'), "
                        + "(9, 7, 4000, 'ADDED', 'Sample data: a made-up item for trying the app.')");

        AppDatabase database = openWithRoom();
        try {
            // Opening runs every migration, and Room checks the result against version 4.
            SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
            assertEquals(4, db.getVersion());

            // One product for each different name, unit, and sample mark, in order of first use.
            assertEquals(Arrays.asList(
                            "1|Granola bars|||bars|0",
                            "2|granola bars|||bars|0",
                            "3|Bandages||||0",
                            "4|Bottled water|||bottles|1"),
                    rows(db, "SELECT id, name, variant, packageSize, unit, sample FROM products ORDER BY id"));

            // Every batch keeps its id, quantity, date, label, location, and sample mark.
            // Dates start unconfirmed, food answers "Not checked" (null), and nothing is archived.
            assertEquals(Arrays.asList(
                            "1|1|FOOD|6|2026-10-12|null|BEST_BY|0|2|null|null|null|null|null|0|0",
                            "2|1|FOOD|10|2027-03-01|null|BEST_BY|0|1|null|null|null|null|null|0|0",
                            "3|2|FOOD|2|null|null|OTHER|0|1|null|null|null|null|null|0|0",
                            "4|3|FIRST_AID_SUPPLY|1|null|null|OTHER|0|1|null|null|null|null|null|0|0",
                            "7|4|WATER|12|2027-01-01|null|BEST_BY|0|2|null|null|null|null|null|0|1"),
                    rows(db, "SELECT id, productId, category, quantity, date, printedDateText, "
                            + "dateLabel, dateConfirmed, locationId, foodType, sealed, shelfStable, "
                            + "damaged, storageConcern, archived, sample FROM batches ORDER BY id"));

            // Every history entry is kept exactly.
            assertEquals(Arrays.asList(
                            "1|1|1000|ADDED|Added 6 bars to Earthquake kit.",
                            "2|1|2000|EDITED|Quantity changed from 8 bars to 6 bars.",
                            "3|2|3000|ADDED|Added 10 bars to Pantry.",
                            "4|3|3500|ADDED|Added 2 bars to Pantry.",
                            "5|4|3600|ADDED|Added 1 to Pantry.",
                            "9|7|4000|ADDED|Sample data: a made-up item for trying the app."),
                    rows(db, "SELECT id, batchId, occurredAtMillis, type, details FROM batch_events ORDER BY id"));

            assertEquals(Collections.emptyList(), rows(db, "PRAGMA foreign_key_check"));
            // Version 3: every product starts "Not classified", and the new tables are empty.
            assertEquals(Arrays.asList("null", "null", "null", "null"),
                    rows(db, "SELECT foodClass FROM products ORDER BY id"));
            assertEquals(Collections.singletonList("0"), rows(db, "SELECT COUNT(*) FROM swap_actions"));
            assertEquals(Collections.singletonList("0"), rows(db, "SELECT COUNT(*) FROM purchase_items"));
            // The helper tables of the migration are gone.
            assertEquals(Collections.emptyList(), rows(db, "SELECT name FROM sqlite_master "
                    + "WHERE name IN ('batches_new', 'batch_events_copy')"));

            // The app's own database code works with the migrated data.
            Batch bars = database.batchDao().getById(1);
            assertNotNull(bars);
            assertEquals(1, bars.productId);
            assertEquals(6, bars.quantity);
            assertFalse(bars.dateConfirmed);

            Product same = database.productDao().findSame("GRANOLA BARS", "", "", "BARS");
            assertNotNull(same);
            assertEquals(1, same.id);

            Batch added = new Batch();
            added.productId = 1;
            added.locationId = 1;
            added.quantity = 3;
            long newId = database.batchDao().insert(added);
            assertTrue("a new batch gets a new id, got " + newId, newId > 7);
        } finally {
            database.close();
        }
    }

    @Test
    public void migrate1To2_withNoBatches_works() {
        createVersion1("INSERT INTO locations (id, name, type) VALUES (1, 'Pantry', 'PANTRY')");

        AppDatabase database = openWithRoom();
        try {
            SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
            assertEquals(4, db.getVersion());
            assertEquals(Collections.singletonList("0"), rows(db, "SELECT COUNT(*) FROM products"));
            assertEquals(Collections.singletonList("0"), rows(db, "SELECT COUNT(*) FROM batches"));
            assertEquals(Collections.singletonList("1|Pantry|PANTRY"),
                    rows(db, "SELECT id, name, type FROM locations"));
        } finally {
            database.close();
        }
    }

    @Test
    public void migrate1To2_whenAStepFails_changesNothing() {
        // A batch that points to a missing location: the migration's last check stops it.
        // (The app never saves such a row; it is made here only to make the migration fail.)
        createVersion1(
                "INSERT INTO locations (id, name, type) VALUES (1, 'Pantry', 'PANTRY')",
                "INSERT INTO batches (id, name, category, quantity, unit, date, dateLabel, "
                        + "locationId, sample) VALUES "
                        + "(1, 'Rice', 'FOOD', 2, 'bags', NULL, 'BEST_BY', 99, 0)",
                "INSERT INTO batch_events (id, batchId, occurredAtMillis, type, details) VALUES "
                        + "(1, 1, 1000, 'ADDED', 'Added 2 bags.')");

        AppDatabase database = openWithRoom();
        try {
            assertThrows(RuntimeException.class, () -> database.getOpenHelper().getWritableDatabase());
        } finally {
            database.close();
        }

        // The file is still version 1, with the old tables and rows.
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DB).getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            assertEquals(1, db.getVersion());
            assertEquals(Collections.singletonList("1|Rice|2|bags|99"),
                    rows(db, "SELECT id, name, quantity, unit, locationId FROM batches"));
            assertEquals(Collections.singletonList("1|1|Added 2 bags."),
                    rows(db, "SELECT id, batchId, details FROM batch_events"));
            assertEquals(Collections.emptyList(), rows(db,
                    "SELECT name FROM sqlite_master WHERE name = 'products'"));
        }
    }

    @Test
    public void migrate2To3_keepsEveryRow_addsAnEmptyFoodClass_andEmptyNewTables() {
        createVersion2(
                "INSERT INTO locations (id, name, type) VALUES "
                        + "(1, 'Pantry', 'PANTRY'), (2, 'Earthquake kit', 'EMERGENCY_KIT'), "
                        + "(3, 'First aid box', 'FIRST_AID')",
                "INSERT INTO products (id, name, variant, packageSize, unit, sample) VALUES "
                        + "(1, 'Canned beans', 'Low sodium', '15 oz', 'cans', 0), "
                        + "(2, 'Pain reliever', '', '', 'tablets', 0), "
                        + "(5, 'Granola bars', '', '', 'bars', 1)",
                // Every column has a value somewhere: dates, printed text, answers, archived, sample.
                "INSERT INTO batches (id, productId, category, quantity, date, printedDateText, "
                        + "dateLabel, dateConfirmed, locationId, foodType, sealed, shelfStable, "
                        + "damaged, storageConcern, archived, sample) VALUES "
                        + "(1, 1, 'FOOD', 6, '2026-11-01', NULL, 'BEST_BY', 1, 2, 'CANNED', 'YES', "
                        + "'YES', 'NO', 'NO', 0, 0), "
                        + "(2, 1, 'FOOD', 12, NULL, '03/2027', 'USE_BY', 0, 1, NULL, NULL, NULL, "
                        + "NULL, NULL, 0, 0), "
                        + "(3, 2, 'MEDICINE', 20, '2027-01-31', NULL, 'EXPIRES', 1, 3, NULL, NULL, "
                        + "NULL, NULL, NULL, 1, 0), "
                        + "(8, 5, 'FOOD', 4, '2027-03-01', NULL, 'OTHER', 1, 1, 'DRY_OR_BOXED', NULL, "
                        + "NULL, NULL, NULL, 0, 1)",
                "INSERT INTO batch_events (id, batchId, occurredAtMillis, type, details) VALUES "
                        + "(1, 1, 1000, 'ADDED', 'Added 6 cans to Earthquake kit.'), "
                        + "(2, 2, 2000, 'ADDED', 'Added 12 cans to Pantry.'), "
                        + "(3, 3, 3000, 'ARCHIVED', 'Archived with 20 tablets in First aid box.'), "
                        + "(7, 8, 4000, 'ADDED', 'Sample data: a made-up item for trying the app.')");
        List<String> oldProducts = rowsBeforeOpening("SELECT * FROM products ORDER BY id");
        List<String> oldBatches = rowsBeforeOpening("SELECT * FROM batches ORDER BY id");
        List<String> oldHistory = rowsBeforeOpening("SELECT * FROM batch_events ORDER BY id");

        AppDatabase database = openWithRoom();
        try {
            SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
            assertEquals(4, db.getVersion());

            // Every old row is kept exactly. Only the starting kit gets its new name (version 4).
            assertEquals(Arrays.asList("1|Pantry|PANTRY", "2|Emergency kit|EMERGENCY_KIT",
                    "3|First aid box|FIRST_AID"), rows(db, "SELECT * FROM locations ORDER BY id"));
            assertEquals(oldProducts, rows(db, "SELECT id, name, variant, packageSize, unit, sample "
                    + "FROM products ORDER BY id"));
            assertEquals(oldBatches, rows(db, "SELECT * FROM batches ORDER BY id"));
            assertEquals(oldHistory, rows(db, "SELECT * FROM batch_events ORDER BY id"));

            // The new column: TEXT, may be empty, no default; every product starts "Not classified".
            assertTrue(rows(db, "PRAGMA table_info(products)").contains("6|foodClass|TEXT|0|null|0"));
            assertEquals(Arrays.asList("null", "null", "null"),
                    rows(db, "SELECT foodClass FROM products ORDER BY id"));
            assertEquals(Collections.singletonList("0"), rows(db, "SELECT COUNT(*) FROM swap_actions"));
            assertEquals(Collections.singletonList("0"), rows(db, "SELECT COUNT(*) FROM purchase_items"));
            assertEquals(Collections.emptyList(), rows(db, "PRAGMA foreign_key_check"));

            // The app's own database code works with the migrated data.
            assertEquals(1, database.productDao().setFoodClass(1, FoodClass.SHELF_STABLE_FOOD));
            BatchInfo beans = database.batchDao().getInfo(1);
            assertNotNull(beans);
            assertEquals(FoodClass.SHELF_STABLE_FOOD, beans.getFoodClass());
            PurchaseItem item = new PurchaseItem();
            item.productId = 1;
            item.kitLocationId = 2;
            item.quantity = 6;
            database.purchaseItemDao().insert(item);
            PurchaseItem sameKey = new PurchaseItem();
            sameKey.productId = 1;
            sameKey.kitLocationId = 2;
            sameKey.quantity = 1;
            assertThrows(RuntimeException.class, () -> database.purchaseItemDao().insert(sameKey));
        } finally {
            database.close();
        }
    }

    @Test
    public void migrate2To3_whenAStepFails_changesNothing() {
        // A products table that already has a foodClass column: the migration's first step
        // (ALTER TABLE ... ADD COLUMN foodClass) fails. (The app never makes such a file; it is
        // made here only to make the migration fail.)
        createVersion2(
                "INSERT INTO locations (id, name, type) VALUES (1, 'Pantry', 'PANTRY')",
                "INSERT INTO products (id, name, variant, packageSize, unit, sample) VALUES "
                        + "(1, 'Canned beans', '', '', 'cans', 0)",
                "INSERT INTO batches (id, productId, category, quantity, date, printedDateText, "
                        + "dateLabel, dateConfirmed, locationId, foodType, sealed, shelfStable, "
                        + "damaged, storageConcern, archived, sample) VALUES "
                        + "(1, 1, 'FOOD', 6, '2026-11-01', NULL, 'BEST_BY', 1, 1, NULL, NULL, NULL, "
                        + "NULL, NULL, 0, 0)",
                "INSERT INTO batch_events (id, batchId, occurredAtMillis, type, details) VALUES "
                        + "(1, 1, 1000, 'ADDED', 'Added 6 cans to Pantry.')",
                "ALTER TABLE products ADD COLUMN foodClass TEXT");
        List<String> oldProducts = rowsBeforeOpening("SELECT * FROM products ORDER BY id");
        List<String> oldBatches = rowsBeforeOpening("SELECT * FROM batches ORDER BY id");
        List<String> oldHistory = rowsBeforeOpening("SELECT * FROM batch_events ORDER BY id");

        AppDatabase database = openWithRoom();
        try {
            assertThrows(RuntimeException.class, () -> database.getOpenHelper().getWritableDatabase());
        } finally {
            database.close();
        }

        // The file is still version 2, with the same rows and none of the new tables.
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DB).getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            assertEquals(2, db.getVersion());
        }
        assertEquals(oldProducts, rowsBeforeOpening("SELECT * FROM products ORDER BY id"));
        assertEquals(oldBatches, rowsBeforeOpening("SELECT * FROM batches ORDER BY id"));
        assertEquals(oldHistory, rowsBeforeOpening("SELECT * FROM batch_events ORDER BY id"));
        assertEquals(Collections.emptyList(), rowsBeforeOpening("SELECT name FROM sqlite_master "
                + "WHERE name IN ('swap_actions', 'purchase_items')"));
    }

    @Test
    public void migrate2To3_withNoBatches_works() {
        createVersion2("INSERT INTO locations (id, name, type) VALUES (1, 'Pantry', 'PANTRY')");

        AppDatabase database = openWithRoom();
        try {
            SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
            assertEquals(4, db.getVersion());
            assertEquals(Collections.singletonList("1|Pantry|PANTRY"),
                    rows(db, "SELECT id, name, type FROM locations"));
            assertEquals(Collections.singletonList("0"), rows(db, "SELECT COUNT(*) FROM products"));
        } finally {
            database.close();
        }
    }

    @Test
    public void migrate3To4_renamesTheStartingKit_andKeepsEveryOtherRow() {
        createVersion3(
                "INSERT INTO locations (id, name, type) VALUES "
                        + "(1, 'Pantry', 'PANTRY'), (2, 'Earthquake kit', 'EMERGENCY_KIT'), "
                        + "(3, 'First aid box', 'FIRST_AID'), (4, 'Car kit', 'EMERGENCY_KIT')",
                "INSERT INTO products (id, name, variant, packageSize, unit, sample, foodClass) "
                        + "VALUES (1, 'Granola bars', 'Oat and honey', '1.5 oz', 'bars', 0, "
                        + "'SHELF_STABLE_FOOD'), (2, 'Water', '', '', 'bottles', 0, NULL)",
                "INSERT INTO batches (id, productId, category, quantity, date, printedDateText, "
                        + "dateLabel, dateConfirmed, locationId, foodType, sealed, shelfStable, "
                        + "damaged, storageConcern, archived, sample) VALUES "
                        + "(1, 1, 'FOOD', 6, '2027-06-09', NULL, 'BEST_BY', 1, 2, 'DRY_OR_BOXED', "
                        + "'YES', 'YES', 'NO', 'NO', 0, 0), "
                        + "(2, 1, 'FOOD', 6, '2026-11-01', NULL, 'BEST_BY', 1, 1, 'DRY_OR_BOXED', "
                        + "'YES', 'YES', 'NO', 'NO', 0, 0), "
                        + "(3, 2, 'WATER', 12, '2027-01-31', NULL, 'BEST_BY', 1, 4, NULL, NULL, NULL, "
                        + "NULL, NULL, 0, 0)",
                "INSERT INTO batch_events (id, batchId, occurredAtMillis, type, details) VALUES "
                        + "(1, 2, 1000, 'ADDED', 'Added 6 bars to Earthquake kit.'), "
                        + "(2, 1, 2000, 'ADDED', 'Added 6 bars to Pantry.'), "
                        + "(3, 2, 3000, 'SWAPPED', 'Swap: moved all 6 bars from Earthquake kit to "
                        + "Pantry, for review.'), "
                        + "(4, 1, 3000, 'SWAPPED', 'Swap: moved all 6 bars from Pantry to "
                        + "Earthquake kit.'), "
                        + "(5, 3, 4000, 'ADDED', 'Added 12 bottles to Car kit.')",
                "INSERT INTO swap_actions (actionId, kitBatchId, pantryBatchId, quantity, planDate, "
                        + "appliedAtMillis) VALUES ('swap-1', 2, 1, 6, '2026-10-12', 3000)",
                "INSERT INTO purchase_items (id, productId, kitLocationId, quantity, addedAtMillis) "
                        + "VALUES (1, 1, 2, 4, 5000), (2, 2, 4, 6, 6000)");
        List<String> oldProducts = rowsBeforeOpening("SELECT * FROM products ORDER BY id");
        List<String> oldBatches = rowsBeforeOpening("SELECT * FROM batches ORDER BY id");
        List<String> oldHistory = rowsBeforeOpening("SELECT * FROM batch_events ORDER BY id");
        List<String> oldSwaps = rowsBeforeOpening("SELECT * FROM swap_actions ORDER BY actionId");
        List<String> oldPurchaseList = rowsBeforeOpening("SELECT * FROM purchase_items ORDER BY id");

        AppDatabase database = openWithRoom();
        try {
            SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
            assertEquals(4, db.getVersion());

            // Only the starting kit's name changes; its id stays, and so does the other kit.
            assertEquals(Arrays.asList("1|Pantry|PANTRY", "2|Emergency kit|EMERGENCY_KIT",
                    "3|First aid box|FIRST_AID", "4|Car kit|EMERGENCY_KIT"),
                    rows(db, "SELECT * FROM locations ORDER BY id"));
            // Every other row is kept exactly, and the history still says "Earthquake kit".
            assertEquals(oldProducts, rows(db, "SELECT * FROM products ORDER BY id"));
            assertEquals(oldBatches, rows(db, "SELECT * FROM batches ORDER BY id"));
            assertEquals(oldHistory, rows(db, "SELECT * FROM batch_events ORDER BY id"));
            assertEquals(oldSwaps, rows(db, "SELECT * FROM swap_actions ORDER BY actionId"));
            assertEquals(oldPurchaseList, rows(db, "SELECT * FROM purchase_items ORDER BY id"));

            // The app's own database code finds the kit by its new name only.
            StorageLocation kit = database.locationDao().findByName("emergency kit");
            assertNotNull(kit);
            assertEquals(2, kit.id);
            assertEquals(0, database.locationDao().countWithName("Earthquake kit"));
            BatchInfo kitBars = database.batchDao().getInfo(1);
            assertNotNull(kitBars);
            assertEquals("Emergency kit", kitBars.getLocationName());
        } finally {
            database.close();
        }
    }

    @Test
    public void migrate3To4_whenAnEmergencyKitExists_renamesNothing() {
        // The household already added a place with the new name (in other capital letters), so
        // the starting kit keeps its name: two places never share a name.
        createVersion3("INSERT INTO locations (id, name, type) VALUES "
                + "(1, 'Pantry', 'PANTRY'), (2, 'Earthquake kit', 'EMERGENCY_KIT'), "
                + "(3, 'First aid box', 'FIRST_AID'), (4, 'emergency KIT', 'OTHER')");
        List<String> oldLocations = rowsBeforeOpening("SELECT * FROM locations ORDER BY id");

        AppDatabase database = openWithRoom();
        try {
            SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
            assertEquals(4, db.getVersion());
            assertEquals(oldLocations, rows(db, "SELECT * FROM locations ORDER BY id"));
        } finally {
            database.close();
        }
    }

    /** Writes a version 1 database file with the given rows. */
    private void createVersion1(String... inserts) {
        createFile(VERSION_1_SCHEMA, 1, inserts);
    }

    /** Writes a version 2 database file with the given rows. */
    private void createVersion2(String... inserts) {
        createFile(VERSION_2_SCHEMA, 2, inserts);
    }

    /** Writes a version 3 database file with the given rows. */
    private void createVersion3(String... inserts) {
        createFile(VERSION_3_SCHEMA, 3, inserts);
    }

    private void createFile(String[] schema, int version, String... inserts) {
        File file = context.getDatabasePath(TEST_DB);
        File folder = file.getParentFile();
        assertNotNull(folder);
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            for (String sql : schema) {
                db.execSQL(sql);
            }
            for (String sql : inserts) {
                db.execSQL(sql);
            }
            db.setVersion(version);
        }
    }

    /** Rows of the test file before Room opens it. */
    private List<String> rowsBeforeOpening(String query) {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DB).getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            return rows(db, query);
        }
    }

    /** Opens the test file the way the app opens its database: Room with the migrations. */
    private AppDatabase openWithRoom() {
        return Room.databaseBuilder(context, AppDatabase.class, TEST_DB)
                .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3,
                        Migrations.MIGRATION_3_4)
                .build();
    }

    /** Each row as its columns joined with "|"; a missing value (NULL) is written "null". */
    private static List<String> rows(SupportSQLiteDatabase db, String query) {
        try (Cursor cursor = db.query(query)) {
            return rows(cursor);
        }
    }

    private static List<String> rows(SQLiteDatabase db, String query) {
        try (Cursor cursor = db.rawQuery(query, null)) {
            return rows(cursor);
        }
    }

    private static List<String> rows(Cursor cursor) {
        List<String> rows = new ArrayList<>();
        while (cursor.moveToNext()) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                if (i > 0) {
                    row.append('|');
                }
                row.append(cursor.isNull(i) ? "null" : cursor.getString(i));
            }
            rows.add(row.toString());
        }
        return rows;
    }
}

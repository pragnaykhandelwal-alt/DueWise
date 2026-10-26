package com.example.readyshelf.data;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * The steps that change an older database into the current version, keeping all household data.
 *
 * <p>Room runs a migration inside one transaction. If any step fails, nothing changes, and the
 * old data stays as it was. The CREATE statements are copied from the schema files in
 * app/schemas/, because Room checks that the result matches them exactly.
 */
public final class Migrations {

    private Migrations() {
        // Only constants, so no objects of this class are made.
    }

    /**
     * Version 1 to 2: adds the "products" table, and batches point to a product instead of
     * keeping their own name and unit. Adds the date, food condition, and archive columns.
     *
     * <ul>
     *   <li>Each different (name, unit, sample) in the old batches becomes one product, with no
     *       variant and no package size. Names that differ only in capital letters stay
     *       separate products, so nothing is merged by guessing.</li>
     *   <li>Every batch keeps its id, quantity, date, date label, location, and sample mark.</li>
     *   <li>Old dates were never confirmed, so dateConfirmed starts as false ("Date needs
     *       review"). Food answers start as "Not checked", and nothing is archived.</li>
     *   <li>Every history entry keeps its id, batch, time, type, and text.</li>
     * </ul>
     *
     * <p>Order of the steps: SQLite can't change a table's columns in place, so "batches" is
     * built again. Dropping a table that history points to (with ON DELETE CASCADE) could
     * delete the history if foreign keys were on, so the history is copied to a plain table
     * first and copied back at the end.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1. Products, one for each different name, unit, and sample mark.
            db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY "
                    + "AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `variant` TEXT NOT NULL, "
                    + "`packageSize` TEXT NOT NULL, `unit` TEXT NOT NULL, `sample` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO products (name, variant, packageSize, unit, sample) "
                    + "SELECT name, '', '', unit, sample FROM batches "
                    + "GROUP BY name, unit, sample ORDER BY MIN(id)");

            // 2. The new batches table, with every old batch copied in under the same id.
            db.execSQL("CREATE TABLE IF NOT EXISTS `batches_new` (`id` INTEGER PRIMARY KEY "
                    + "AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `category` TEXT NOT NULL, "
                    + "`quantity` INTEGER NOT NULL, `date` TEXT, `printedDateText` TEXT, "
                    + "`dateLabel` TEXT NOT NULL, `dateConfirmed` INTEGER NOT NULL, "
                    + "`locationId` INTEGER NOT NULL, `foodType` TEXT, `sealed` TEXT, "
                    + "`shelfStable` TEXT, `damaged` TEXT, `storageConcern` TEXT, "
                    + "`archived` INTEGER NOT NULL, `sample` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION , "
                    + "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION )");
            // "=" compares text exactly (capital letters count), like the GROUP BY above.
            db.execSQL("INSERT INTO batches_new (id, productId, category, quantity, date, "
                    + "printedDateText, dateLabel, dateConfirmed, locationId, foodType, sealed, "
                    + "shelfStable, damaged, storageConcern, archived, sample) "
                    + "SELECT b.id, p.id, b.category, b.quantity, b.date, NULL, b.dateLabel, 0, "
                    + "b.locationId, NULL, NULL, NULL, NULL, NULL, 0, b.sample "
                    + "FROM batches AS b JOIN products AS p "
                    + "ON p.name = b.name AND p.unit = b.unit AND p.sample = b.sample");
            requireSameCount(db, "batches", "batches_new");

            // 3. A plain copy of the history, with no foreign key.
            db.execSQL("CREATE TABLE `batch_events_copy` AS SELECT id, batchId, occurredAtMillis, "
                    + "type, details FROM batch_events");
            requireSameCount(db, "batch_events", "batch_events_copy");

            // 4. Replace the old tables. History goes first, so nothing points to the old batches.
            db.execSQL("DROP TABLE `batch_events`");
            db.execSQL("DROP TABLE `batches`");
            db.execSQL("ALTER TABLE `batches_new` RENAME TO `batches`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_batches_locationId` ON `batches` (`locationId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_batches_productId` ON `batches` (`productId`)");

            // 5. The history table again, with every entry copied back under the same id.
            db.execSQL("CREATE TABLE IF NOT EXISTS `batch_events` (`id` INTEGER PRIMARY KEY "
                    + "AUTOINCREMENT NOT NULL, `batchId` INTEGER NOT NULL, "
                    + "`occurredAtMillis` INTEGER NOT NULL, `type` TEXT NOT NULL, "
                    + "`details` TEXT NOT NULL, FOREIGN KEY(`batchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("INSERT INTO batch_events (id, batchId, occurredAtMillis, type, details) "
                    + "SELECT id, batchId, occurredAtMillis, type, details FROM batch_events_copy");
            requireSameCount(db, "batch_events_copy", "batch_events");
            db.execSQL("DROP TABLE `batch_events_copy`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_batch_events_batchId` ON `batch_events` (`batchId`)");

            // 6. Every batch must point to a real product and location, and every history
            //    entry to a real batch. A problem stops the migration, so nothing changes.
            try (Cursor problems = db.query("PRAGMA foreign_key_check")) {
                if (problems.moveToFirst()) {
                    throw new IllegalStateException("Migration 1 to 2: a row points to a "
                            + "missing row in table " + problems.getString(2));
                }
            }
        }
    };

    /**
     * Version 2 to 3: adds the product food class, the swap receipts, and the purchase list.
     *
     * <ul>
     *   <li>Every product gets an empty food class (null, "Not classified"). The household
     *       chooses one on the batch form; nothing is guessed.</li>
     *   <li>The two new tables start empty. No existing row is copied or changed, so every
     *       batch, product, location, and history entry stays exactly as it was.</li>
     * </ul>
     *
     * <p>The ALTER TABLE has no DEFAULT and no NOT NULL, so the new column matches the column
     * that Room creates for a new install (a TEXT column that may be empty).
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1. The food class of each product.
            db.execSQL("ALTER TABLE `products` ADD COLUMN `foodClass` TEXT");

            // 2. Receipts of confirmed swaps, one for each action ID.
            db.execSQL("CREATE TABLE IF NOT EXISTS `swap_actions` (`actionId` TEXT NOT NULL, "
                    + "`kitBatchId` INTEGER NOT NULL, `pantryBatchId` INTEGER NOT NULL, "
                    + "`quantity` INTEGER NOT NULL, `planDate` TEXT NOT NULL, "
                    + "`appliedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`actionId`), "
                    + "FOREIGN KEY(`kitBatchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE , "
                    + "FOREIGN KEY(`pantryBatchId`) REFERENCES `batches`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_swap_actions_kitBatchId` "
                    + "ON `swap_actions` (`kitBatchId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_swap_actions_pantryBatchId` "
                    + "ON `swap_actions` (`pantryBatchId`)");

            // 3. The purchase list, with one item at most for each product and kit.
            db.execSQL("CREATE TABLE IF NOT EXISTS `purchase_items` (`id` INTEGER PRIMARY KEY "
                    + "AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, "
                    + "`kitLocationId` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, "
                    + "`addedAtMillis` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE , "
                    + "FOREIGN KEY(`kitLocationId`) REFERENCES `locations`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE NO ACTION )");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "`index_purchase_items_productId_kitLocationId` "
                    + "ON `purchase_items` (`productId`, `kitLocationId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_items_kitLocationId` "
                    + "ON `purchase_items` (`kitLocationId`)");
        }
    };

    /**
     * Version 3 to 4: the starting kit is called "Emergency kit" instead of "Earthquake kit"
     * (the student's choice, 2026-10-22). No table changes.
     *
     * <ul>
     *   <li>Only the starting kit is renamed: the place named exactly "Earthquake kit" with the
     *       type Emergency kit. Every other place keeps its name.</li>
     *   <li>If a place called "Emergency kit" already exists (capital letters don't matter),
     *       nothing is renamed, so two places never share a name.</li>
     *   <li>Batches, swap receipts, and the purchase list point to a place by its id, so they
     *       show the new name. History entries keep their text, so older ones still say
     *       "Earthquake kit".</li>
     * </ul>
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("UPDATE locations SET name = 'Emergency kit' "
                    + "WHERE name = 'Earthquake kit' AND type = 'EMERGENCY_KIT' "
                    + "AND NOT EXISTS (SELECT 1 FROM locations "
                    + "WHERE name = 'Emergency kit' COLLATE NOCASE)");
        }
    };

    /** Stops the migration when a copy didn't get every row. */
    private static void requireSameCount(@NonNull SupportSQLiteDatabase db, @NonNull String from,
                                         @NonNull String to) {
        long fromCount = count(db, from);
        long toCount = count(db, to);
        if (fromCount != toCount) {
            throw new IllegalStateException("Migration 1 to 2: copied " + toCount + " of "
                    + fromCount + " rows from " + from + " to " + to);
        }
    }

    private static long count(@NonNull SupportSQLiteDatabase db, @NonNull String table) {
        try (Cursor cursor = db.query("SELECT COUNT(*) FROM `" + table + "`")) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }
}

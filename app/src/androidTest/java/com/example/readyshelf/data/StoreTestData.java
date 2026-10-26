package com.example.readyshelf.data;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.readyshelf.domain.ConditionAnswer;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.FoodClass;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.RotationPlan;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.domain.SwapProposal;
import com.example.readyshelf.domain.PlanBatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helpers for the storage tests: a database in memory, made-up rows, and a full copy of every
 * table, so a test can prove that nothing changed. The rows are made up; they are not real
 * household data, and they never touch the app's own database file.
 */
final class StoreTestData {

    private StoreTestData() {
        // Only static methods and nested classes.
    }

    /** A new, empty database in memory, with the app's tables (version 4). */
    @NonNull
    static AppDatabase inMemory(@NonNull Context context) {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
    }

    static long location(@NonNull AppDatabase db, @NonNull String name, @NonNull LocationType type) {
        StorageLocation location = new StorageLocation();
        location.name = name;
        location.type = type;
        return db.locationDao().insert(location);
    }

    static long product(@NonNull AppDatabase db, @NonNull String name, @NonNull String unit,
                        @Nullable FoodClass foodClass, boolean sample) {
        Product product = new Product();
        product.name = name;
        product.unit = unit;
        product.foodClass = foodClass;
        product.sample = sample;
        return db.productDao().insert(product);
    }

    /**
     * A food batch with a confirmed complete date that is sealed, not damaged, and has no
     * storage concern.
     */
    static long foodBatch(@NonNull AppDatabase db, long productId, long locationId, int quantity,
                          @NonNull LocalDate date, boolean sample) {
        Batch batch = new Batch();
        batch.productId = productId;
        batch.locationId = locationId;
        batch.category = ItemCategory.FOOD;
        batch.quantity = quantity;
        batch.date = date;
        batch.dateLabel = DateLabel.BEST_BY;
        batch.dateConfirmed = true;
        batch.sealed = ConditionAnswer.YES;
        batch.damaged = ConditionAnswer.NO;
        batch.storageConcern = ConditionAnswer.NO;
        batch.sample = sample;
        return db.batchDao().insert(batch);
    }

    /**
     * A batch of any category with a confirmed complete date (Best by) and no food type or
     * answers, as the form saves every batch since 2026-10-22.
     */
    static long batch(@NonNull AppDatabase db, long productId, long locationId,
                      @NonNull ItemCategory category, int quantity, @NonNull LocalDate date) {
        Batch batch = new Batch();
        batch.productId = productId;
        batch.locationId = locationId;
        batch.category = category;
        batch.quantity = quantity;
        batch.date = date;
        batch.dateLabel = DateLabel.BEST_BY;
        batch.dateConfirmed = true;
        return db.batchDao().insert(batch);
    }

    /** Every row of every household table, as text. Two equal dumps mean nothing changed. */
    @NonNull
    static List<String> dump(@NonNull AppDatabase db) {
        SupportSQLiteDatabase sql = db.getOpenHelper().getReadableDatabase();
        List<String> rows = new ArrayList<>();
        for (String table : new String[] {"locations", "products", "batches", "batch_events",
                "swap_actions", "purchase_items"}) {
            String order = table.equals("swap_actions") ? "actionId" : "id";
            rows.add("[" + table + "]");
            rows.addAll(rows(sql, "SELECT * FROM " + table + " ORDER BY " + order));
        }
        return rows;
    }

    /** Each row as its columns joined with "|"; a missing value (NULL) is written "null". */
    @NonNull
    static List<String> rows(@NonNull SupportSQLiteDatabase sql, @NonNull String query) {
        List<String> rows = new ArrayList<>();
        try (Cursor cursor = sql.query(query)) {
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
        }
        return rows;
    }

    static long count(@NonNull AppDatabase db, @NonNull String table) {
        try (Cursor cursor = db.getOpenHelper().getReadableDatabase()
                .query("SELECT COUNT(*) FROM " + table)) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }

    /** What a test planner answers for one call. */
    interface Answer {
        @NonNull
        RotationPlan answer(@NonNull LocalDate today, int nearDateDays, int replacementDays);
    }

    /**
     * A planner that gives the answer each test sets up, and remembers its last call.
     * It contains no swap rules: it never looks at the batches it is given.
     */
    static final class PresetPlanner implements RotationPlannerContract {
        private final Answer answer;
        int calls;
        @Nullable LocalDate lastToday;
        int lastNearDateDays;
        int lastReplacementDays;

        PresetPlanner(@NonNull Answer answer) {
            this.answer = answer;
        }

        /** Always proposes the same one swap. */
        @NonNull
        static PresetPlanner always(long kitBatchId, long pantryBatchId, int quantity) {
            RotationPlan plan = StoreTestData.plan(kitBatchId, pantryBatchId, quantity);
            return new PresetPlanner((today, near, replacement) -> plan);
        }

        @NonNull
        @Override
        public RotationPlan plan(@NonNull List<PlanBatch> batches, @NonNull LocalDate today,
                                 int nearDateDays, int replacementDays) {
            calls++;
            lastToday = today;
            lastNearDateDays = nearDateDays;
            lastReplacementDays = replacementDays;
            return answer.answer(today, nearDateDays, replacementDays);
        }
    }

    /** A plan with one proposed swap and no notes. */
    @NonNull
    static RotationPlan plan(long kitBatchId, long pantryBatchId, int quantity) {
        return new RotationPlan(Collections.singletonList(
                new SwapProposal(kitBatchId, pantryBatchId, quantity)), Collections.emptyList());
    }

    /** A plan with nothing in it. */
    @NonNull
    static RotationPlan emptyPlan() {
        return new RotationPlan(Collections.emptyList(), Collections.emptyList());
    }
}

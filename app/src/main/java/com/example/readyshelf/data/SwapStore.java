package com.example.readyshelf.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.domain.RuleRunner;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Saves a swap that the household confirmed on the Review swap screen.
 *
 * <p>Everything happens in one database transaction: the checks, both movements, their history,
 * and the receipt. If anything fails, the transaction is rolled back, so neither movement is
 * saved. ReadyShelfRepository calls {@link #confirm} on its background thread; tests call it
 * directly.
 *
 * <p>Room commits a transaction when its code returns normally. So every answer except SAVED is
 * returned before the first write, and a problem after the first write throws.
 */
public final class SwapStore {

    private static final String TAG = "SwapStore";

    /** What happened to a confirmed swap. */
    public enum Result {
        /** Both movements, their history, and the receipt were saved. */
        SAVED,
        /** This action ID was saved before (a repeated tap), so nothing changed now. */
        ALREADY_SAVED,
        /** Nothing was saved: one of the two batches changed or disappeared after it was shown. */
        BATCH_CHANGED,
        /**
         * Nothing was saved: the swap rules no longer propose this swap with today's date, the
         * current settings, and the current stock.
         */
        NO_LONGER_PROPOSED,
        /** Nothing was saved: the database or the swap rules reported an error. */
        FAILED
    }

    /** One confirmed swap: what the household saw, and the history texts to save with it. */
    public static final class Request {
        @NonNull private final String actionId;
        @NonNull private final RuleRunner.ShownSwap shown;
        @NonNull private final String kitHistory;
        @Nullable private final String kitPartHistory;
        @NonNull private final String pantryHistory;
        @Nullable private final String pantryPartHistory;

        /**
         * @param actionId          made once by the Review swap screen; the same for every tap
         * @param shown             the swap exactly as the screen showed it
         * @param kitHistory        history for the kit batch
         * @param kitPartHistory    history for the new batch made from the part of the kit batch
         *                          that moves; needed when only part of it moves
         * @param pantryHistory     history for the pantry batch
         * @param pantryPartHistory history for the new batch made from the part of the pantry
         *                          batch that moves; needed when only part of it moves
         */
        public Request(@NonNull String actionId, @NonNull RuleRunner.ShownSwap shown,
                       @NonNull String kitHistory, @Nullable String kitPartHistory,
                       @NonNull String pantryHistory, @Nullable String pantryPartHistory) {
            if (actionId.trim().isEmpty()) {
                throw new IllegalArgumentException("A swap needs an action ID");
            }
            this.actionId = actionId;
            this.shown = Objects.requireNonNull(shown, "shown");
            this.kitHistory = Objects.requireNonNull(kitHistory, "kitHistory");
            this.kitPartHistory = kitPartHistory;
            this.pantryHistory = Objects.requireNonNull(pantryHistory, "pantryHistory");
            this.pantryPartHistory = pantryPartHistory;
        }

        @NonNull
        public String getActionId() {
            return actionId;
        }

        @NonNull
        public RuleRunner.ShownSwap getShown() {
            return shown;
        }
    }

    private final AppDatabase database;

    public SwapStore(@NonNull AppDatabase database) {
        this.database = database;
    }

    /**
     * Checks the swap again and saves it, all in one transaction.
     *
     * @param today    the phone's date, read inside the transaction
     * @param settings the current Settings numbers, read inside the transaction
     */
    @WorkerThread
    @NonNull
    public Result confirm(@NonNull Request request, @NonNull RotationPlannerContract planner,
                          @NonNull Supplier<LocalDate> today,
                          @NonNull Supplier<DateThresholds> settings) {
        try {
            return database.runInTransaction(() -> checkAndSave(request, planner, today, settings));
        } catch (RuntimeException error) {
            // The transaction was rolled back, so neither movement was saved.
            Log.e(TAG, "A swap could not be saved", error);
            return Result.FAILED;
        }
    }

    @NonNull
    private Result checkAndSave(@NonNull Request request, @NonNull RotationPlannerContract planner,
                                @NonNull Supplier<LocalDate> today,
                                @NonNull Supplier<DateThresholds> settings) {
        RuleRunner.ShownSwap shown = request.shown;
        BatchInfo kitShown = shown.getKitBatch();
        BatchInfo pantryShown = shown.getPantryBatch();
        int quantity = shown.getQuantity();

        // 1. A repeated tap (or a retry after the screen was closed) brings the same action ID.
        SwapAction receipt = database.swapActionDao().find(request.actionId);
        if (receipt != null) {
            if (receipt.kitBatchId == kitShown.getId() && receipt.pantryBatchId == pantryShown.getId()
                    && receipt.quantity == quantity) {
                return Result.ALREADY_SAVED;
            }
            throw new IllegalStateException("Action ID " + request.actionId
                    + " was already used for a different swap");
        }

        // 2. Both batches must be exactly as the household saw them: quantity, date, label,
        //    confirmation, product and its food class, location, category, and food answers.
        if (!kitShown.equals(database.batchDao().getInfo(kitShown.getId()))
                || !pantryShown.equals(database.batchDao().getInfo(pantryShown.getId()))) {
            return Result.BATCH_CHANGED;
        }

        // 3. The swap rules must still propose this swap with today's date and the current
        //    settings. After midnight or a settings change, an unchanged swap can stop qualifying.
        LocalDate planDate = today.get();
        RuleRunner.ShownPlan fresh = RuleRunner.plan(database.batchDao().getActiveInfo(), planner,
                planDate, settings.get());
        if (!fresh.getSwaps().contains(shown)) {
            return Result.NO_LONGER_PROPOSED;
        }

        // Nothing is written before this line. From here on a problem throws, and the
        // transaction is rolled back.
        Batch kit = requireBatch(kitShown.getId());
        Batch pantry = requireBatch(pantryShown.getId());
        long kitLocationId = kit.locationId;
        long pantryLocationId = pantry.locationId;
        long now = System.currentTimeMillis();

        // 4. and 5. Both movements with their history.
        moveUnits(kit, quantity, pantryLocationId, request.kitHistory, request.kitPartHistory, now);
        moveUnits(pantry, quantity, kitLocationId, request.pantryHistory, request.pantryPartHistory,
                now);

        // 6. The receipt, so the same action ID can't save this swap again.
        SwapAction done = new SwapAction();
        done.actionId = request.actionId;
        done.kitBatchId = kitShown.getId();
        done.pantryBatchId = pantryShown.getId();
        done.quantity = quantity;
        done.planDate = planDate;
        done.appliedAtMillis = now;
        database.swapActionDao().insert(done);
        return Result.SAVED;
    }

    /**
     * Moves {@code amount} units of a batch to another location. Moving all of it changes the
     * batch's location. Moving part of it lowers the batch and adds a new batch for the part,
     * with the same product, date, label type, confirmation, and food answers.
     */
    private void moveUnits(@NonNull Batch batch, int amount, long toLocationId,
                           @NonNull String history, @Nullable String partHistory, long now) {
        if (amount < 1 || amount > batch.quantity) {
            throw new IllegalStateException("Batch " + batch.id + " has " + batch.quantity
                    + ", so " + amount + " can't move");
        }
        if (amount == batch.quantity) {
            batch.locationId = toLocationId;
            requireOneRow(database.batchDao().update(batch), batch.id);
        } else {
            if (partHistory == null) {
                throw new IllegalArgumentException("Moving part of batch " + batch.id
                        + " needs history for the part that moves");
            }
            Batch part = batch.copyWithoutId();
            part.quantity = amount;
            part.locationId = toLocationId;
            batch.quantity -= amount;
            requireOneRow(database.batchDao().update(batch), batch.id);
            long partId = database.batchDao().insert(part);
            database.batchEventDao().insert(
                    BatchEvent.of(partId, BatchEventType.SWAPPED, partHistory, now));
        }
        database.batchEventDao().insert(BatchEvent.of(batch.id, BatchEventType.SWAPPED, history, now));
    }

    @NonNull
    private Batch requireBatch(long batchId) {
        Batch batch = database.batchDao().getById(batchId);
        if (batch == null) {
            throw new IllegalStateException("Batch " + batchId + " disappeared during the swap");
        }
        return batch;
    }

    private static void requireOneRow(int changedRows, long batchId) {
        if (changedRows != 1) {
            throw new IllegalStateException("Batch " + batchId + " was not saved");
        }
    }
}

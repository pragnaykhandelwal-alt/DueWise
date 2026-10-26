package com.example.readyshelf.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.readyshelf.domain.DateEntry;
import com.example.readyshelf.domain.StockChange;

import java.util.concurrent.Callable;

/**
 * Saves changes to the purchase list. ReadyShelfRepository calls these methods on its
 * background thread; tests call them directly.
 *
 * <p>A purchase-list item is only a note. Adding, changing, or removing one never touches
 * the batches, so it never changes recorded stock. Stock changes only when a bought item is
 * recorded as a new batch with its printed date ({@link #recordPurchase}).
 *
 * <p>Each method runs in one transaction. Every answer except SAVED is returned before the
 * first write, and any error rolls everything back and gives FAILED.
 */
public final class PurchaseListStore {

    private static final String TAG = "PurchaseListStore";

    /** What happened to a purchase-list change. */
    public enum Result {
        SAVED,
        /** Nothing was saved: this product and place already have an item on the list. */
        ALREADY_ON_LIST,
        /** Nothing was saved: the item is gone, or its amount changed after it was shown. */
        ITEM_CHANGED,
        /** Nothing was saved: the database reported an error, or the change was not allowed. */
        FAILED
    }

    private final AppDatabase database;

    public PurchaseListStore(@NonNull AppDatabase database) {
        this.database = database;
    }

    /**
     * Adds an item for one product and one place. The product and place are the item's key:
     * if the list already has an item with that key, nothing is added. The place must exist,
     * and sample products can't be added. Since 2026-10-22 the place can be any place, because
     * expired items in the pantry and other places need replacement too; until then it had to
     * be an Emergency kit or First aid location. (The place is stored as kitLocationId.)
     */
    @WorkerThread
    @NonNull
    public Result add(long productId, long kitLocationId, int quantity) {
        return run(() -> {
            requireQuantity(quantity);
            Product product = database.productDao().getById(productId);
            StorageLocation place = database.locationDao().getById(kitLocationId);
            if (product == null || product.sample) {
                throw new IllegalArgumentException("Product " + productId + " can't be added");
            }
            if (place == null) {
                throw new IllegalArgumentException("Location " + kitLocationId + " doesn't exist");
            }
            if (database.purchaseItemDao().findByKey(productId, kitLocationId) != null) {
                return Result.ALREADY_ON_LIST;
            }
            PurchaseItem item = new PurchaseItem();
            item.productId = productId;
            item.kitLocationId = kitLocationId;
            item.quantity = quantity;
            item.addedAtMillis = System.currentTimeMillis();
            database.purchaseItemDao().insert(item);
            return Result.SAVED;
        });
    }

    /** Changes an item's amount, but only while it is still {@code expectedQuantity}. */
    @WorkerThread
    @NonNull
    public Result changeQuantity(long itemId, int expectedQuantity, int newQuantity) {
        return run(() -> {
            requireQuantity(newQuantity);
            return database.purchaseItemDao().changeQuantity(itemId, expectedQuantity, newQuantity) == 1
                    ? Result.SAVED : Result.ITEM_CHANGED;
        });
    }

    /** Removes an item. ITEM_CHANGED means it was already gone. */
    @WorkerThread
    @NonNull
    public Result remove(long itemId) {
        return run(() -> database.purchaseItemDao().delete(itemId) == 1
                ? Result.SAVED : Result.ITEM_CHANGED);
    }

    /**
     * Records a bought item: adds {@code newBatch} as a new batch of the item's product with
     * an "Added" history entry, and removes the item from the list, in one transaction.
     *
     * <p>The batch must have its printed date: a complete date or the printed text of an
     * incomplete one ("No date printed" is not allowed). Its quantity must be from 1 to 9999.
     * The product comes from the item, not from {@code newBatch}. When the item is already gone
     * (for example after a second tap), nothing is added and the answer is ITEM_CHANGED.
     */
    @WorkerThread
    @NonNull
    public Result recordPurchase(long itemId, @NonNull Batch newBatch,
                                 @NonNull String historyDetails) {
        return run(() -> {
            if (DateEntry.kindOf(newBatch.date, newBatch.printedDateText) == DateEntry.Kind.NONE) {
                throw new IllegalArgumentException("A bought item needs its printed date");
            }
            requireQuantity(newBatch.quantity);
            PurchaseItem item = database.purchaseItemDao().getById(itemId);
            if (item == null) {
                return Result.ITEM_CHANGED;
            }
            Product product = database.productDao().getById(item.productId);
            if (product == null) {
                throw new IllegalStateException("Product " + item.productId + " is missing");
            }

            // Nothing is written before this line.
            newBatch.id = 0;
            newBatch.productId = item.productId;
            newBatch.sample = product.sample;
            newBatch.archived = false;
            long batchId = database.batchDao().insert(newBatch);
            database.batchEventDao().insert(BatchEvent.of(batchId, BatchEventType.ADDED,
                    historyDetails, System.currentTimeMillis()));
            if (database.purchaseItemDao().delete(itemId) != 1) {
                throw new IllegalStateException("Purchase item " + itemId + " was not removed");
            }
            return Result.SAVED;
        });
    }

    /** Runs one change in a transaction. An error rolls it back and gives FAILED. */
    @NonNull
    private Result run(@NonNull Callable<Result> change) {
        try {
            return database.runInTransaction(change);
        } catch (RuntimeException error) {
            Log.e(TAG, "A purchase-list change could not be saved", error);
            return Result.FAILED;
        }
    }

    private static void requireQuantity(int quantity) {
        if (quantity < 1 || quantity > StockChange.MAX_QUANTITY) {
            throw new IllegalArgumentException("The amount must be from 1 to "
                    + StockChange.MAX_QUANTITY + ", not " + quantity);
        }
    }
}

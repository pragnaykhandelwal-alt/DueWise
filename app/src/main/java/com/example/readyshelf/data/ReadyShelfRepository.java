package com.example.readyshelf.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.room.InvalidationTracker;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.RotationPlannerContract;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The one place where screens read and change the data in one database file: the household's
 * own records, or the separate sample data (see DataSets).
 *
 * <p>Reading: the methods return LiveData, and Room keeps it up to date.
 * Changing: the methods return right away and do the work on a background thread,
 * because Android doesn't allow database work on the main (screen) thread. When the
 * work is done, Room sends new values to the LiveData, and the screens update by themselves.
 */
public final class ReadyShelfRepository {

    private static final String TAG = "ReadyShelfRepository";

    /** What happened to a change that the household confirmed. */
    public enum SaveResult {
        SAVED,
        /** Nothing was saved: the batch changed or disappeared after the review was shown. */
        BATCH_CHANGED,
        /** Nothing was saved: the database reported an error. */
        FAILED
    }

    /** Hears how a change ended. It is called on the background thread. */
    public interface SaveListener {
        void onResult(@NonNull SaveResult result);
    }

    /** Hears how a confirmed swap ended. It is called on the background thread. */
    public interface SwapListener {
        void onResult(@NonNull SwapStore.Result result);
    }

    /** Hears how a purchase-list change ended. It is called on the background thread. */
    public interface PurchaseListener {
        void onResult(@NonNull PurchaseListStore.Result result);
    }

    /**
     * Hears how copying the sample data ended, and how many batches were copied (0 unless
     * SAVED). It is called on the background thread.
     */
    public interface CopyListener {
        void onResult(@NonNull SaveResult result, int copiedBatches);
    }

    private final AppDatabase database;
    private final SwapStore swapStore;
    private final PurchaseListStore purchaseListStore;
    // One background thread, so changes happen one at a time, in order.
    private final ExecutorService background = Executors.newSingleThreadExecutor();

    public ReadyShelfRepository(@NonNull AppDatabase database) {
        this.database = database;
        swapStore = new SwapStore(database);
        purchaseListStore = new PurchaseListStore(database);
    }

    // ----- Reading -----

    @NonNull
    public LiveData<List<StorageLocation>> observeLocations() {
        return database.locationDao().observeAll();
    }

    /** Every product, sample products too, sorted by name. */
    @NonNull
    public LiveData<List<Product>> observeProducts() {
        return database.productDao().observeAll();
    }

    /** Every batch that is not archived. */
    @NonNull
    public LiveData<List<BatchInfo>> observeBatches() {
        return database.batchDao().observeActiveInfo();
    }

    /** Every batch, archived ones too. */
    @NonNull
    public LiveData<List<BatchInfo>> observeBatchesWithArchived() {
        return database.batchDao().observeAllInfo();
    }

    /** One batch with its product and location. The value is null when the batch doesn't exist. */
    @NonNull
    public LiveData<BatchInfo> observeBatchInfo(long batchId) {
        return database.batchDao().observeInfo(batchId);
    }

    /** One batch's history, newest first. */
    @NonNull
    public LiveData<List<BatchEvent>> observeHistory(long batchId) {
        return database.batchEventDao().observeForBatch(batchId);
    }

    @NonNull
    public LiveData<Integer> observeSampleCount() {
        return database.batchDao().observeSampleCount();
    }

    /**
     * Every batch that is not archived, read now. Background thread only: the daily reminder
     * check calls it from WorkManager's thread.
     */
    @WorkerThread
    @NonNull
    public List<BatchInfo> activeBatchesNow() {
        return database.batchDao().getActiveInfo();
    }

    /**
     * Calls {@code listener} after each saved change to this file's batches, on one of Room's
     * background threads. The home-screen widget uses it, so a change on any screen refreshes it.
     * Background thread only: Room opens the database file to start listening.
     */
    @WorkerThread
    public void listenForBatchChanges(@NonNull Runnable listener) {
        // "batches" is the table name in Batch's @Entity. A batch's status depends only on it.
        database.getInvalidationTracker().addObserver(
                new InvalidationTracker.Observer("batches") {
                    @Override
                    public void onInvalidated(@NonNull Set<String> tables) {
                        listener.run();
                    }
                });
    }

    /** Every item on the purchase list, sorted by product name and then kit. */
    @NonNull
    public LiveData<List<PurchaseItemInfo>> observePurchaseItems() {
        return database.purchaseItemDao().observeAll();
    }

    /** The receipt of the swap with this action ID. The value is null until it is saved. */
    @NonNull
    public LiveData<SwapAction> observeSwapReceipt(@NonNull String actionId) {
        return database.swapActionDao().observe(actionId);
    }

    // ----- Changing (each one runs on the background thread) -----

    /**
     * Adds a new batch and its first history entry together.
     *
     * @param newProduct a product to add first, or null when {@code batch.productId} is set. If
     *                   the household already has a product with the same details, that product
     *                   is used instead of adding a second one (the form doesn't let this happen).
     */
    public void addBatch(@Nullable Product newProduct, @NonNull Batch batch,
                         @NonNull String historyDetails, @NonNull SaveListener listener) {
        runChange(() -> {
            if (newProduct != null) {
                batch.productId = productIdFor(newProduct);
            }
            long batchId = database.batchDao().insert(batch);
            database.batchEventDao().insert(newEvent(batchId, BatchEventType.ADDED, historyDetails));
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Saves the Edit form and adds one history entry. Only the form's values change: the
     * product, category, quantity, date, printed text, label type, and confirmation. The
     * location, archived mark, sample mark, food type, and condition answers stay as they are in
     * the database (the form no longer shows the last two; since 2026-10-22 they are kept even
     * when the category changes). Nothing is saved if the batch is gone or its quantity is no
     * longer {@code expectedQuantity} (the quantity the form started from).
     */
    public void updateBatch(@Nullable Product newProduct, @NonNull Batch edited,
                            int expectedQuantity, @NonNull String historyDetails,
                            @NonNull SaveListener listener) {
        runChange(() -> {
            Batch current = database.batchDao().getById(edited.id);
            if (current == null || current.quantity != expectedQuantity) {
                return SaveResult.BATCH_CHANGED;
            }
            current.productId = newProduct != null ? productIdFor(newProduct) : edited.productId;
            current.category = edited.category;
            current.quantity = edited.quantity;
            current.date = edited.date;
            current.printedDateText = edited.printedDateText;
            current.dateLabel = edited.dateLabel;
            current.dateConfirmed = edited.dateConfirmed;
            database.batchDao().update(current);
            database.batchEventDao().insert(newEvent(current.id, BatchEventType.EDITED, historyDetails));
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Records that {@code amount} of a batch was used or discarded. Nothing is saved if the
     * batch is gone, archived, or no longer has {@code expectedQuantity}.
     *
     * @param type USED or DISCARDED
     */
    public void removeFromBatch(long batchId, int expectedQuantity, int amount,
                                @NonNull BatchEventType type, @NonNull String historyDetails,
                                @NonNull SaveListener listener) {
        if (type != BatchEventType.USED && type != BatchEventType.DISCARDED) {
            throw new IllegalArgumentException("Not a removal: " + type);
        }
        runChange(() -> {
            Batch current = database.batchDao().getById(batchId);
            if (!canChangeStock(current, expectedQuantity, amount)) {
                return SaveResult.BATCH_CHANGED;
            }
            current.quantity -= amount;
            database.batchDao().update(current);
            database.batchEventDao().insert(newEvent(batchId, type, historyDetails));
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Moves {@code amount} of a batch to another location. Moving all of it changes the
     * batch's location. Moving some of it lowers this batch and adds a new batch with the same
     * product, date, and details at the new location. Both batches get a history entry.
     * Nothing is saved if the batch is gone, archived, or no longer has {@code expectedQuantity}.
     *
     * @param newBatchDetails history for the new batch; needed when only some of it moves
     */
    public void moveBatch(long batchId, int expectedQuantity, int amount, long toLocationId,
                          @NonNull String historyDetails, @Nullable String newBatchDetails,
                          @NonNull SaveListener listener) {
        runChange(() -> {
            Batch current = database.batchDao().getById(batchId);
            if (!canChangeStock(current, expectedQuantity, amount)) {
                return SaveResult.BATCH_CHANGED;
            }
            if (current.locationId == toLocationId) {
                throw new IllegalArgumentException("A move needs a different location");
            }
            if (amount == current.quantity) {
                current.locationId = toLocationId;
                database.batchDao().update(current);
            } else {
                if (newBatchDetails == null) {
                    throw new IllegalArgumentException("Moving some needs history for the new batch");
                }
                Batch movedPart = current.copyWithoutId();
                movedPart.quantity = amount;
                movedPart.locationId = toLocationId;
                current.quantity -= amount;
                database.batchDao().update(current);
                long newBatchId = database.batchDao().insert(movedPart);
                database.batchEventDao().insert(newEvent(newBatchId, BatchEventType.MOVED, newBatchDetails));
            }
            database.batchEventDao().insert(newEvent(batchId, BatchEventType.MOVED, historyDetails));
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Archives a batch: it keeps its quantity and history, but Inventory hides it and the rules
     * don't see it. Nothing is saved if the batch is gone, already archived, or no longer has
     * {@code expectedQuantity}.
     */
    public void archiveBatch(long batchId, int expectedQuantity, @NonNull String historyDetails,
                             @NonNull SaveListener listener) {
        runChange(() -> {
            Batch current = database.batchDao().getById(batchId);
            if (current == null || current.archived || current.quantity != expectedQuantity) {
                return SaveResult.BATCH_CHANGED;
            }
            current.archived = true;
            database.batchDao().update(current);
            database.batchEventDao().insert(newEvent(batchId, BatchEventType.ARCHIVED, historyDetails));
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * "Renewed" on the Purchase/Renewal list (added 2026-10-24): the batch gets {@code newDate}
     * as its complete, confirmed date, with the same label type, and one history entry (Edited).
     * Its quantity, location, and everything else stay as they are. Nothing is saved if the batch
     * is gone, archived, or no longer has {@code expectedQuantity} and {@code expectedDate} (what
     * the screen showed).
     */
    public void renewBatch(long batchId, int expectedQuantity, @Nullable LocalDate expectedDate,
                           @NonNull LocalDate newDate, @NonNull String historyDetails,
                           @NonNull SaveListener listener) {
        runChange(() -> {
            Batch current = database.batchDao().getById(batchId);
            if (current == null || current.archived || current.quantity != expectedQuantity
                    || !Objects.equals(current.date, expectedDate)) {
                return SaveResult.BATCH_CHANGED;
            }
            current.date = newDate;
            current.printedDateText = null;
            current.dateConfirmed = true;
            database.batchDao().update(current);
            database.batchEventDao().insert(newEvent(batchId, BatchEventType.EDITED, historyDetails));
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * "Discard" on the Purchase/Renewal list (added 2026-10-24, the student's choice "Discard
     * and archive"): records the whole quantity as discarded, then archives the batch, with two
     * history entries (Recorded as discarded, then Archived), in one transaction. Nothing is
     * saved if the batch is gone, archived, has nothing left, or no longer has
     * {@code expectedQuantity}.
     */
    public void discardAndArchive(long batchId, int expectedQuantity,
                                  @NonNull String discardDetails, @NonNull String archiveDetails,
                                  @NonNull SaveListener listener) {
        runChange(() -> {
            Batch current = database.batchDao().getById(batchId);
            if (!canChangeStock(current, expectedQuantity, expectedQuantity)) {
                return SaveResult.BATCH_CHANGED;
            }
            current.quantity = 0;
            current.archived = true;
            database.batchDao().update(current);
            database.batchEventDao().insert(newEvent(batchId, BatchEventType.DISCARDED, discardDetails));
            database.batchEventDao().insert(newEvent(batchId, BatchEventType.ARCHIVED, archiveDetails));
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Saves a swap that the household confirmed: it checks the swap again and saves both
     * movements, their history, and the receipt in one transaction (see SwapStore).
     *
     * @param today    gives the phone's date when the check runs ({@code LocalDate::now})
     * @param settings gives the current Settings numbers when the check runs
     */
    public void confirmSwap(@NonNull SwapStore.Request request,
                            @NonNull RotationPlannerContract planner,
                            @NonNull Supplier<LocalDate> today,
                            @NonNull Supplier<DateThresholds> settings,
                            @NonNull SwapListener listener) {
        background.execute(() -> listener.onResult(swapStore.confirm(request, planner, today, settings)));
    }

    /** Adds a suggestion to the purchase list. It doesn't change any stock. */
    public void addToPurchaseList(long productId, long kitLocationId, int quantity,
                                  @NonNull PurchaseListener listener) {
        background.execute(() -> listener.onResult(
                purchaseListStore.add(productId, kitLocationId, quantity)));
    }

    /** Changes the amount of a purchase-list item. It doesn't change any stock. */
    public void changePurchaseQuantity(long itemId, int expectedQuantity, int newQuantity,
                                       @NonNull PurchaseListener listener) {
        background.execute(() -> listener.onResult(
                purchaseListStore.changeQuantity(itemId, expectedQuantity, newQuantity)));
    }

    /** Removes an item from the purchase list. It doesn't change any stock. */
    public void removeFromPurchaseList(long itemId, @NonNull PurchaseListener listener) {
        background.execute(() -> listener.onResult(purchaseListStore.remove(itemId)));
    }

    /**
     * Records a bought item as a new batch and removes it from the purchase list, together
     * (see PurchaseListStore.recordPurchase).
     */
    public void recordPurchase(long itemId, @NonNull Batch newBatch,
                               @NonNull String historyDetails,
                               @NonNull PurchaseListener listener) {
        background.execute(() -> listener.onResult(purchaseListStore.recordPurchase(itemId,
                newBatch, historyDetails)));
    }

    /**
     * Adds a location, unless one with the same name exists (capital letters don't matter).
     * The result is SAVED when the location exists afterwards, or FAILED when the database
     * reported an error (then nothing changed).
     */
    public void addLocation(@NonNull String name, @NonNull LocationType type,
                            @NonNull SaveListener listener) {
        runChange(() -> {
            if (database.locationDao().countWithName(name) == 0) {
                StorageLocation location = new StorageLocation();
                location.name = name;
                location.type = type;
                database.locationDao().insert(location);
            }
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Sample-data file only: replaces everything in it with the three starting locations and
     * fresh sample data dated from {@code today} (see SampleData), in one transaction.
     *
     * <p>It first checks that this repository's database really is a sample-data file
     * ({@link AppDatabase#isSampleDataFile}). For any other database, the household's too, it
     * changes nothing and the result is FAILED. The result is also FAILED when the database
     * reported an error (then nothing changed).
     */
    public void replaceWithSampleData(@NonNull LocalDate today, @NonNull String historyDetails,
                                      @NonNull SaveListener listener) {
        runChange(() -> {
            emptySampleDataFile();
            Map<LocationType, Long> locationIds = new EnumMap<>(LocationType.class);
            for (AppDatabase.StartingLocation start : AppDatabase.STARTING_LOCATIONS) {
                StorageLocation location = new StorageLocation();
                location.name = start.name;
                location.type = start.type;
                locationIds.put(start.type, database.locationDao().insert(location));
            }
            List<SampleData.Item> items = SampleData.create(today,
                    locationIds.get(LocationType.PANTRY), locationIds.get(LocationType.EMERGENCY_KIT),
                    locationIds.get(LocationType.FIRST_AID));
            // Batches of the same product share one Product object; add each product once.
            Map<Product, Long> productIds = new IdentityHashMap<>();
            for (SampleData.Item item : items) {
                Long productId = productIds.get(item.product);
                if (productId == null) {
                    productId = database.productDao().insert(item.product);
                    productIds.put(item.product, productId);
                }
                item.batch.productId = productId;
                long batchId = database.batchDao().insert(item.batch);
                database.batchEventDao().insert(newEvent(batchId, BatchEventType.ADDED, historyDetails));
            }
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Sample-data file only: deletes everything in it, in one transaction. For any other
     * database, the household's too, it changes nothing and the result is FAILED.
     */
    public void clearSampleData(@NonNull SaveListener listener) {
        runChange(() -> {
            emptySampleDataFile();
            return SaveResult.SAVED;
        }, listener);
    }

    /**
     * Deletes every row of every table, after checking that this is a sample-data file.
     * Call it inside a transaction, as its first step.
     *
     * @throws IllegalStateException for any other database; then nothing is deleted
     */
    private void emptySampleDataFile() {
        if (!AppDatabase.isSampleDataFile(database)) {
            throw new IllegalStateException("Only a sample-data file can be emptied");
        }
        SampleResetDao reset = database.sampleResetDao();
        reset.deleteSwapActions();
        reset.deletePurchaseItems();
        reset.deleteBatchEvents();
        reset.deleteBatches();
        reset.deleteProducts();
        reset.deleteLocations();
    }

    /**
     * The household's file only: copies everything in the sample data ({@code sampleData}) into
     * this file, as the household's own records, in one transaction. The household asked for this
     * with "Keep these items as my own" in Settings, until that card was removed on 2026-10-22.
     * <ul>
     *   <li>Every batch is copied, archived ones too, with every value as it is (dates, their
     *       confirmation, food condition answers), and its history. It is no longer marked
     *       "Sample item", and it gets one more history entry, {@code historyDetails}.</li>
     *   <li>A place with the same name (capital letters don't matter) is used instead of adding
     *       a second one. Other places are added.</li>
     *   <li>A product with the same details is used, and keeps its own food class. Other
     *       products are added, no longer marked sample.</li>
     *   <li>The purchase list is copied too. An entry this file already has for the same product
     *       and kit is kept, and the copied one is left out.</li>
     *   <li>Swap receipts are not copied: each batch's history already says what was swapped.</li>
     * </ul>
     * Nothing that is already in this file changes. The result is FAILED, and nothing is copied,
     * when this is a sample-data file, when {@code sampleData} is not one, or when the database
     * reported an error.
     */
    public void copyFromSampleData(@NonNull ReadyShelfRepository sampleData,
                                   @NonNull String historyDetails, @NonNull CopyListener listener) {
        int[] copied = new int[1];
        runChange(() -> {
            copied[0] = copyEverythingFrom(sampleData.database, historyDetails);
            return SaveResult.SAVED;
        }, result -> listener.onResult(result, result == SaveResult.SAVED ? copied[0] : 0));
    }

    /**
     * The copy of {@link #copyFromSampleData}. Call it inside this file's transaction. Returns how
     * many batches were copied.
     *
     * @throws IllegalStateException when this is a sample-data file or {@code from} is not one;
     *                               then nothing is copied
     */
    private int copyEverythingFrom(@NonNull AppDatabase from, @NonNull String historyDetails) {
        if (AppDatabase.isSampleDataFile(database) || !AppDatabase.isSampleDataFile(from)) {
            throw new IllegalStateException(
                    "Only sample data can be copied, and only into the household's records");
        }
        // Places: the household's place with the same name, or a new one.
        Map<Long, Long> locationIds = new HashMap<>();
        for (StorageLocation place : from.locationDao().getAll()) {
            StorageLocation same = database.locationDao().findByName(place.name);
            if (same != null) {
                locationIds.put(place.id, same.id);
            } else {
                StorageLocation copy = new StorageLocation();
                copy.name = place.name;
                copy.type = place.type;
                locationIds.put(place.id, database.locationDao().insert(copy));
            }
        }
        // Products: the household's product with the same details, or a new one (not sample).
        Map<Long, Long> productIds = new HashMap<>();
        for (Product product : from.productDao().getAll()) {
            Product copy = new Product();
            copy.name = product.name;
            copy.variant = product.variant;
            copy.packageSize = product.packageSize;
            copy.unit = product.unit;
            copy.foodClass = product.foodClass;
            productIds.put(product.id, productIdFor(copy));
        }
        // Batches, each with its history and one more entry that says where it came from.
        int copied = 0;
        long now = System.currentTimeMillis();
        for (Batch batch : from.batchDao().getAll()) {
            Batch copy = batch.copyWithoutId();
            copy.productId = copiedId(productIds, batch.productId);
            copy.locationId = copiedId(locationIds, batch.locationId);
            copy.sample = false;
            long batchId = database.batchDao().insert(copy);
            for (BatchEvent event : from.batchEventDao().getForBatch(batch.id)) {
                database.batchEventDao().insert(BatchEvent.of(batchId, event.type, event.details,
                        event.occurredAtMillis));
            }
            database.batchEventDao().insert(
                    BatchEvent.of(batchId, BatchEventType.ADDED, historyDetails, now));
            copied++;
        }
        // The purchase list. The household's own entry for the same product and kit is kept.
        for (PurchaseItem item : from.purchaseItemDao().getAll()) {
            long productId = copiedId(productIds, item.productId);
            long kitId = copiedId(locationIds, item.kitLocationId);
            if (database.purchaseItemDao().findByKey(productId, kitId) == null) {
                PurchaseItem copy = new PurchaseItem();
                copy.productId = productId;
                copy.kitLocationId = kitId;
                copy.quantity = item.quantity;
                copy.addedAtMillis = item.addedAtMillis;
                database.purchaseItemDao().insert(copy);
            }
        }
        return copied;
    }

    /** The id in this file of a copied place or product. */
    private static long copiedId(@NonNull Map<Long, Long> copiedIds, long sampleId) {
        Long id = copiedIds.get(sampleId);
        if (id == null) {
            throw new IllegalStateException("Nothing was copied for id " + sampleId);
        }
        return id;
    }

    /**
     * Deletes every sample batch and its history, then the sample products no batch uses.
     * Since 2026-10-12 sample data has a file of its own, so this is only for sample items
     * that an earlier version added to the household's records. The household's own batches
     * and products don't change. The result is FAILED when the database reported an error
     * (then nothing changed).
     */
    public void removeSampleData(@NonNull SaveListener listener) {
        runChange(() -> {
            database.batchDao().deleteSample();
            database.productDao().deleteUnusedSample();
            return SaveResult.SAVED;
        }, listener);
    }

    /** Runs one change in a transaction on the background thread, then tells the listener. */
    private void runChange(@NonNull Callable<SaveResult> change, @NonNull SaveListener listener) {
        background.execute(() -> {
            SaveResult result;
            try {
                result = database.runInTransaction(change);
            } catch (RuntimeException error) {
                // The transaction was rolled back, so nothing changed.
                Log.e(TAG, "A change could not be saved", error);
                result = SaveResult.FAILED;
            }
            listener.onResult(result);
        });
    }

    /** True when {@code amount} can leave this batch: it exists, isn't archived, and is unchanged. */
    private static boolean canChangeStock(@Nullable Batch current, int expectedQuantity, int amount) {
        return current != null && !current.archived && current.quantity == expectedQuantity
                && amount >= 1 && amount <= current.quantity;
    }

    /** The id of the household's product with these details, adding the product if needed. */
    private long productIdFor(@NonNull Product product) {
        Product same = database.productDao().findSame(product.name, product.variant,
                product.packageSize, product.unit);
        return same != null ? same.id : database.productDao().insert(product);
    }

    private static BatchEvent newEvent(long batchId, BatchEventType type, String details) {
        return BatchEvent.of(batchId, type, details, System.currentTimeMillis());
    }
}

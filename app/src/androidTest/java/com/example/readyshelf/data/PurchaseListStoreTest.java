package com.example.readyshelf.data;

import static com.example.readyshelf.data.StoreTestData.count;
import static com.example.readyshelf.data.StoreTestData.dump;
import static com.example.readyshelf.data.StoreTestData.foodBatch;
import static com.example.readyshelf.data.StoreTestData.location;
import static com.example.readyshelf.data.StoreTestData.product;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.List;

/**
 * Storage tests for the purchase list (PurchaseListStore): one item per product and place, list
 * items never change stock, and a bought item enters the stock only as a new batch with its
 * printed date. Each test uses a database in memory with made-up rows.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class PurchaseListStoreTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AppDatabase db;
    private PurchaseListStore store;
    private long pantryId;
    private long kitId;
    private long beansId;

    @Before
    public void createDatabase() {
        db = StoreTestData.inMemory(context);
        store = new PurchaseListStore(db);
        pantryId = location(db, "Pantry", LocationType.PANTRY);
        kitId = location(db, "Earthquake kit", LocationType.EMERGENCY_KIT);
        beansId = product(db, "Canned beans", "cans", null, false);
        foodBatch(db, beansId, kitId, 5, DAY.minusDays(1), false);
    }

    @After
    public void closeDatabase() {
        db.close();
    }

    // ----- Adding, changing, and removing items never changes stock -----

    @Test
    public void add_storesOneItem_andChangesNoStock() {
        List<String> stockBefore = stock();

        assertEquals(PurchaseListStore.Result.SAVED, store.add(beansId, kitId, 5));

        assertEquals(1, count(db, "purchase_items"));
        PurchaseItem item = db.purchaseItemDao().findByKey(beansId, kitId);
        assertNotNull(item);
        assertEquals(5, item.quantity);
        assertEquals(stockBefore, stock());
    }

    @Test
    public void addingTheSameProductAndKitTwice_keepsOneItem() {
        assertEquals(PurchaseListStore.Result.SAVED, store.add(beansId, kitId, 5));
        List<String> before = dump(db);

        assertEquals(PurchaseListStore.Result.ALREADY_ON_LIST, store.add(beansId, kitId, 3));

        assertEquals(before, dump(db));
        assertEquals(1, count(db, "purchase_items"));
    }

    @Test
    public void theUniqueKey_refusesASecondItemForTheSameProductAndKit() {
        db.purchaseItemDao().insert(item(beansId, kitId, 5));

        assertThrows(SQLiteConstraintException.class,
                () -> db.purchaseItemDao().insert(item(beansId, kitId, 2)));
        assertEquals(1, count(db, "purchase_items"));
    }

    @Test
    public void theSameProductForAnotherKit_isASeparateItem() {
        long carKit = location(db, "Car kit", LocationType.EMERGENCY_KIT);

        assertEquals(PurchaseListStore.Result.SAVED, store.add(beansId, kitId, 5));
        assertEquals(PurchaseListStore.Result.SAVED, store.add(beansId, carKit, 2));

        assertEquals(2, count(db, "purchase_items"));
    }

    @Test
    public void add_forThePantry_isSaved() {
        // Since 2026-10-22 an item can be for any place: expired pantry food needs replacement
        // too. Until then only kits were allowed.
        assertEquals(PurchaseListStore.Result.SAVED, store.add(beansId, pantryId, 5));

        assertEquals(1, count(db, "purchase_items"));
    }

    @Test
    public void add_forAMissingPlace_aSampleProduct_orNoAmount_savesNothing() {
        long sampleBeans = product(db, "Sample beans", "cans", null, true);
        List<String> before = dump(db);

        assertEquals(PurchaseListStore.Result.FAILED, store.add(beansId, 9_999L, 5));
        assertEquals(PurchaseListStore.Result.FAILED, store.add(sampleBeans, kitId, 5));
        assertEquals(PurchaseListStore.Result.FAILED, store.add(beansId, kitId, 0));

        assertEquals(before, dump(db));
    }

    @Test
    public void changeQuantity_savesTheNewAmount_andChangesNoStock() {
        store.add(beansId, kitId, 5);
        long itemId = itemId();
        List<String> stockBefore = stock();

        assertEquals(PurchaseListStore.Result.SAVED, store.changeQuantity(itemId, 5, 3));

        assertEquals(3, db.purchaseItemDao().getById(itemId).quantity);
        assertEquals(stockBefore, stock());
    }

    @Test
    public void changeQuantity_whenTheAmountChangedAfterItWasShown_savesNothing() {
        store.add(beansId, kitId, 5);
        long itemId = itemId();
        List<String> before = dump(db);

        assertEquals(PurchaseListStore.Result.ITEM_CHANGED, store.changeQuantity(itemId, 4, 3));

        assertEquals(before, dump(db));
    }

    @Test
    public void remove_deletesTheItemOnce_andChangesNoStock() {
        store.add(beansId, kitId, 5);
        long itemId = itemId();
        List<String> stockBefore = stock();

        assertEquals(PurchaseListStore.Result.SAVED, store.remove(itemId));
        assertEquals(PurchaseListStore.Result.ITEM_CHANGED, store.remove(itemId));

        assertEquals(0, count(db, "purchase_items"));
        assertEquals(stockBefore, stock());
    }

    // ----- Recording a bought item -----

    @Test
    public void recordPurchase_addsANewBatchWithItsHistory_andRemovesTheItem() {
        store.add(beansId, kitId, 5);
        long itemId = itemId();
        Batch bought = boughtBatch(DAY.plusDays(400), null);
        bought.productId = 999; // ignored: the product comes from the item

        assertEquals(PurchaseListStore.Result.SAVED,
                store.recordPurchase(itemId, bought, "Added 5 cans to Pantry. Bought from the purchase list."));

        assertNull(db.purchaseItemDao().getById(itemId));
        assertEquals(2, count(db, "batches"));
        String boughtDate = DAY.plusDays(400).toString(); // 2027-11-15
        assertEquals(java.util.Collections.singletonList(
                        beansId + "|FOOD|5|" + boughtDate + "|null|" + pantryId + "|0|0"),
                StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                        "SELECT productId, category, quantity, date, printedDateText, locationId, "
                                + "archived, sample FROM batches WHERE date = '" + boughtDate + "'"));
        assertEquals(java.util.Collections.singletonList(
                        "ADDED|Added 5 cans to Pantry. Bought from the purchase list."),
                StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                        "SELECT type, details FROM batch_events"));
    }

    @Test
    public void recordPurchase_withOnlyThePrintedText_isAccepted() {
        store.add(beansId, kitId, 5);

        assertEquals(PurchaseListStore.Result.SAVED,
                store.recordPurchase(itemId(), boughtBatch(null, "03/2028"), "history"));

        assertEquals(java.util.Collections.singletonList("03/2028"),
                StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                        "SELECT printedDateText FROM batches WHERE printedDateText IS NOT NULL"));
    }

    @Test
    public void recordPurchase_withoutAPrintedDate_savesNothing() {
        store.add(beansId, kitId, 5);
        List<String> before = dump(db);

        assertEquals(PurchaseListStore.Result.FAILED,
                store.recordPurchase(itemId(), boughtBatch(null, null), "history"));
        assertEquals(PurchaseListStore.Result.FAILED,
                store.recordPurchase(itemId(), boughtBatch(null, "   "), "history"));

        assertEquals(before, dump(db));
    }

    @Test
    public void recordPurchaseTwice_addsOneBatch() {
        store.add(beansId, kitId, 5);
        long itemId = itemId();
        assertEquals(PurchaseListStore.Result.SAVED,
                store.recordPurchase(itemId, boughtBatch(DAY.plusDays(400), null), "history"));
        List<String> afterFirst = dump(db);

        assertEquals(PurchaseListStore.Result.ITEM_CHANGED,
                store.recordPurchase(itemId, boughtBatch(DAY.plusDays(400), null), "history"));

        assertEquals(afterFirst, dump(db));
    }

    @Test
    public void recordPurchase_whenRemovingTheItemFails_savesNothing() {
        store.add(beansId, kitId, 5);
        List<String> before = dump(db);
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER fail_item_removal "
                + "BEFORE DELETE ON purchase_items BEGIN SELECT RAISE(ABORT, 'made-up failure'); END");

        assertEquals(PurchaseListStore.Result.FAILED,
                store.recordPurchase(itemId(), boughtBatch(DAY.plusDays(400), null), "history"));

        assertEquals(before, dump(db));
    }

    // ----- Helpers -----

    /** The batches and their history: what "recorded stock" means. */
    @NonNull
    private List<String> stock() {
        List<String> rows = StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT * FROM batches ORDER BY id");
        rows.addAll(StoreTestData.rows(db.getOpenHelper().getReadableDatabase(),
                "SELECT * FROM batch_events ORDER BY id"));
        return rows;
    }

    private long itemId() {
        PurchaseItem item = db.purchaseItemDao().findByKey(beansId, kitId);
        assertNotNull(item);
        return item.id;
    }

    @NonNull
    private static PurchaseItem item(long productId, long kitLocationId, int quantity) {
        PurchaseItem item = new PurchaseItem();
        item.productId = productId;
        item.kitLocationId = kitLocationId;
        item.quantity = quantity;
        return item;
    }

    /** The batch the "Record bought item" form would save: 5 cans in the pantry. */
    @NonNull
    private Batch boughtBatch(@Nullable LocalDate date, @Nullable String printedText) {
        Batch batch = new Batch();
        batch.productId = beansId;
        batch.category = ItemCategory.FOOD;
        batch.quantity = 5;
        batch.date = date;
        batch.printedDateText = printedText;
        batch.dateLabel = DateLabel.BEST_BY;
        batch.locationId = pantryId;
        return batch;
    }
}

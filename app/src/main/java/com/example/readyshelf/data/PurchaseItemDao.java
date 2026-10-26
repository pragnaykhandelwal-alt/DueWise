package com.example.readyshelf.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Database access for the purchase list. Room writes the code for these methods.
 * The methods that don't return LiveData must be called on a background thread.
 */
@Dao
public interface PurchaseItemDao {

    // Every column gets a name that matches a PurchaseItemInfo constructor parameter.
    String SELECT_ITEM_INFO = "SELECT i.id AS id, i.productId AS productId, p.name AS name, "
            + "p.variant AS variant, p.packageSize AS packageSize, p.unit AS unit, "
            + "i.kitLocationId AS kitLocationId, l.name AS locationName, "
            + "i.quantity AS quantity, i.addedAtMillis AS addedAtMillis "
            + "FROM purchase_items AS i "
            + "JOIN products AS p ON p.id = i.productId "
            + "JOIN locations AS l ON l.id = i.kitLocationId";

    String BY_NAME = " ORDER BY p.name COLLATE NOCASE, l.name COLLATE NOCASE, i.id";

    /** Every item on the purchase list, sorted by product name and then kit. */
    @Query(SELECT_ITEM_INFO + BY_NAME)
    LiveData<List<PurchaseItemInfo>> observeAll();

    @Nullable
    @Query("SELECT * FROM purchase_items WHERE id = :itemId")
    PurchaseItem getById(long itemId);

    /** Every item, oldest first, read now. Background thread only. */
    @Query("SELECT * FROM purchase_items ORDER BY id")
    List<PurchaseItem> getAll();

    /** The item for this product and kit, or null. */
    @Nullable
    @Query("SELECT * FROM purchase_items WHERE productId = :productId AND kitLocationId = :kitLocationId")
    PurchaseItem findByKey(long productId, long kitLocationId);

    /** Adds an item and returns its new id. A second item with the same key fails. */
    @Insert
    long insert(PurchaseItem item);

    /**
     * Changes the amount, but only while it is still {@code expectedQuantity}.
     * Returns how many rows changed (0 if the item is gone or its amount changed).
     */
    @Query("UPDATE purchase_items SET quantity = :newQuantity "
            + "WHERE id = :itemId AND quantity = :expectedQuantity")
    int changeQuantity(long itemId, int expectedQuantity, int newQuantity);

    /** Removes an item. Returns how many rows were deleted (0 if it was already gone). */
    @Query("DELETE FROM purchase_items WHERE id = :itemId")
    int delete(long itemId);
}

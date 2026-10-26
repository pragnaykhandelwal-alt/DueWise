package com.example.readyshelf.data;

import androidx.room.Dao;
import androidx.room.Query;

/**
 * Empties every table, so the sample data can start over. Room writes the code for these methods.
 *
 * <p>Only for the separate sample-data file. ReadyShelfRepository calls these methods only after
 * {@link AppDatabase#isSampleDataFile} said yes, inside one transaction, so the household's
 * file can never be emptied. Call them in the order they are listed: rows that point to other
 * rows go first, because a product or a location can't be deleted while a batch or a
 * purchase-list item still points to it. Background thread only.
 */
@Dao
interface SampleResetDao {

    @Query("DELETE FROM swap_actions")
    void deleteSwapActions();

    @Query("DELETE FROM purchase_items")
    void deletePurchaseItems();

    @Query("DELETE FROM batch_events")
    void deleteBatchEvents();

    @Query("DELETE FROM batches")
    void deleteBatches();

    @Query("DELETE FROM products")
    void deleteProducts();

    @Query("DELETE FROM locations")
    void deleteLocations();
}

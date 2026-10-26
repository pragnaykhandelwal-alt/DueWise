package com.example.readyshelf.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.readyshelf.domain.BatchInfo;

import java.util.List;

/**
 * Database access for batches. Room writes the code for these methods.
 *
 * <p>Methods that return LiveData run in the background by themselves and send new
 * results whenever the tables change. The other methods must be called on a
 * background thread (ReadyShelfRepository does that).
 */
@Dao
public interface BatchDao {

    // Every column gets a name (an alias) that matches a BatchInfo constructor parameter.
    // All three tables have an "id" column, and two have a "name" column, so the aliases
    // also keep them apart.
    String SELECT_BATCH_INFO = "SELECT b.id AS id, b.productId AS productId, p.name AS name, "
            + "p.variant AS variant, p.packageSize AS packageSize, p.unit AS unit, "
            + "p.foodClass AS foodClass, "
            + "b.category AS category, b.quantity AS quantity, b.date AS date, "
            + "b.printedDateText AS printedDateText, b.dateLabel AS dateLabel, "
            + "b.dateConfirmed AS dateConfirmed, b.locationId AS locationId, "
            + "l.name AS locationName, l.type AS locationType, b.foodType AS foodType, "
            + "b.sealed AS sealed, b.shelfStable AS shelfStable, b.damaged AS damaged, "
            + "b.storageConcern AS storageConcern, b.archived AS archived, b.sample AS sample "
            + "FROM batches AS b "
            + "JOIN products AS p ON p.id = b.productId "
            + "JOIN locations AS l ON l.id = b.locationId";

    String BY_NAME = " ORDER BY p.name COLLATE NOCASE, b.id";

    /** Every batch that is not archived. The rules and most screens use this list. */
    @Query(SELECT_BATCH_INFO + " WHERE b.archived = 0" + BY_NAME)
    LiveData<List<BatchInfo>> observeActiveInfo();

    /** Every batch, archived ones too, for Inventory's "Show archived batches". */
    @Query(SELECT_BATCH_INFO + BY_NAME)
    LiveData<List<BatchInfo>> observeAllInfo();

    /** One batch with its product and location. The value is null when the batch doesn't exist. */
    @Query(SELECT_BATCH_INFO + " WHERE b.id = :batchId")
    LiveData<BatchInfo> observeInfo(long batchId);

    /**
     * Every batch that is not archived, in the same order as {@link #observeActiveInfo()}, so a
     * check inside a transaction gives the rules the same list the screen gave them.
     * Background thread only.
     */
    @Query(SELECT_BATCH_INFO + " WHERE b.archived = 0" + BY_NAME)
    List<BatchInfo> getActiveInfo();

    /** One batch with its product and location, or null. Background thread only. */
    @Nullable
    @Query(SELECT_BATCH_INFO + " WHERE b.id = :batchId")
    BatchInfo getInfo(long batchId);

    @Nullable
    @Query("SELECT * FROM batches WHERE id = :batchId")
    Batch getById(long batchId);

    /** Every batch, archived ones too, oldest first. Background thread only. */
    @Query("SELECT * FROM batches ORDER BY id")
    List<Batch> getAll();

    @Query("SELECT COUNT(*) FROM batches WHERE sample = 1")
    LiveData<Integer> observeSampleCount();

    /** Adds a batch and returns its new id. */
    @Insert
    long insert(Batch batch);

    /** Saves changes to a batch. Returns how many rows changed (0 if the batch is gone). */
    @Update
    int update(Batch batch);

    /** Deletes every sample batch. Their history is deleted too (CASCADE). */
    @Query("DELETE FROM batches WHERE sample = 1")
    int deleteSample();
}

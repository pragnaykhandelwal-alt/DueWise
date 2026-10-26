package com.example.readyshelf.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/** Database access for batch history. Room writes the code for these methods. */
@Dao
public interface BatchEventDao {

    /** One batch's history, newest first. */
    @Query("SELECT * FROM batch_events WHERE batchId = :batchId ORDER BY occurredAtMillis DESC, id DESC")
    LiveData<List<BatchEvent>> observeForBatch(long batchId);

    /** One batch's history, oldest first, read now. Background thread only. */
    @Query("SELECT * FROM batch_events WHERE batchId = :batchId ORDER BY occurredAtMillis, id")
    List<BatchEvent> getForBatch(long batchId);

    /** Adds a history entry. Background thread only. */
    @Insert
    long insert(BatchEvent event);
}

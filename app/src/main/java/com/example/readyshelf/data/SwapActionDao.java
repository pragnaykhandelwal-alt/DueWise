package com.example.readyshelf.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/** Database access for swap receipts. Room writes the code for these methods. */
@Dao
public interface SwapActionDao {

    /** The receipt with this action ID, or null. Background thread only. */
    @Nullable
    @Query("SELECT * FROM swap_actions WHERE actionId = :actionId")
    SwapAction find(String actionId);

    /** The receipt with this action ID. The value is null until the swap is saved. */
    @Query("SELECT * FROM swap_actions WHERE actionId = :actionId")
    LiveData<SwapAction> observe(String actionId);

    /** Adds a receipt. A second receipt with the same action ID fails. Background thread only. */
    @Insert
    void insert(SwapAction action);
}

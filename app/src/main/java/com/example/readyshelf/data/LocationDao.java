package com.example.readyshelf.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.readyshelf.domain.LocationType;

import java.util.List;

/** Database access for locations. Room writes the code for these methods. */
@Dao
public interface LocationDao {

    /** All locations, oldest first, so the three starting locations come first. */
    @Query("SELECT * FROM locations ORDER BY id")
    LiveData<List<StorageLocation>> observeAll();

    /** All locations, oldest first, read now. Background thread only. */
    @Query("SELECT * FROM locations ORDER BY id")
    List<StorageLocation> getAll();

    /** The oldest location with this name, ignoring capital letters, or null. Background thread only. */
    @Nullable
    @Query("SELECT * FROM locations WHERE name = :name COLLATE NOCASE ORDER BY id LIMIT 1")
    StorageLocation findByName(String name);

    /** One location, or null. Background thread only. */
    @Nullable
    @Query("SELECT * FROM locations WHERE id = :locationId")
    StorageLocation getById(long locationId);

    /** The first location of this type, or null. Call it on a background thread. */
    @Nullable
    @Query("SELECT * FROM locations WHERE type = :type ORDER BY id LIMIT 1")
    StorageLocation findFirstOfType(LocationType type);

    /** How many locations have this name, ignoring capital letters. Background thread only. */
    @Query("SELECT COUNT(*) FROM locations WHERE name = :name COLLATE NOCASE")
    int countWithName(String name);

    /** Adds a location and returns its new id. Background thread only. */
    @Insert
    long insert(StorageLocation location);
}

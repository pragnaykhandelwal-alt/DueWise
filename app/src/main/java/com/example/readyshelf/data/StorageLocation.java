package com.example.readyshelf.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.readyshelf.domain.LocationType;

/**
 * One row of the "locations" table: a place where batches are kept.
 *
 * <p>Named StorageLocation so it isn't mixed up with Android's GPS class
 * android.location.Location.
 */
@Entity(tableName = "locations")
public class StorageLocation {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @NonNull
    public LocationType type = LocationType.OTHER;
}

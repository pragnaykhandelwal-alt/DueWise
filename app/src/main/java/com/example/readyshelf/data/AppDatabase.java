package com.example.readyshelf.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.readyshelf.domain.LocationType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The ReadyShelf database, saved in the app's private folder on the phone.
 *
 * <p>Version 4. Version 1 had no products table; version 2 had no food class, swap receipts,
 * or purchase list; version 3 has the same tables as version 4, but its starting kit was called
 * "Earthquake kit". The Room Gradle plugin saves each version's schema in app/schemas/.
 * When a table changes, raise the version and add a Migration so household data is kept
 * (see Migrations). Never use fallbackToDestructiveMigration: it deletes all data
 * (CLAUDE.md, rule 5).
 *
 * <p>Two files use these tables: the household's own records ({@link #FILE_NAME}) and the
 * separate sample data ({@link #SAMPLE_FILE_NAME}, see DataSets). Both get the same migrations.
 */
@Database(entities = {StorageLocation.class, Product.class, Batch.class, BatchEvent.class,
        SwapAction.class, PurchaseItem.class},
        version = 4,
        exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    /** The household's own records. */
    public static final String FILE_NAME = "readyshelf.db";

    /**
     * Every sample-data file name ends like this. Only a file with such a name can be emptied
     * (see {@link #isSampleDataFile}), so the household's file never can.
     */
    static final String SAMPLE_FILE_ENDING = "-sample-data.db";

    /** The separate sample data: made-up items, kept apart from the household's records. */
    public static final String SAMPLE_FILE_NAME = "readyshelf" + SAMPLE_FILE_ENDING;

    /** One of the places a new database starts with. */
    static final class StartingLocation {
        @NonNull final String name;
        @NonNull final LocationType type;

        StartingLocation(@NonNull String name, @NonNull LocationType type) {
            this.name = name;
            this.type = type;
        }
    }

    /**
     * The three places a new database starts with. The sample data starts with them too.
     * Until 2026-10-22 the kit was "Earthquake kit"; MIGRATION_3_4 renames it in saved data.
     */
    static final List<StartingLocation> STARTING_LOCATIONS = Collections.unmodifiableList(
            Arrays.asList(new StartingLocation("Pantry", LocationType.PANTRY),
                    new StartingLocation("Emergency kit", LocationType.EMERGENCY_KIT),
                    new StartingLocation("First aid box", LocationType.FIRST_AID)));

    public abstract LocationDao locationDao();

    public abstract ProductDao productDao();

    public abstract BatchDao batchDao();

    public abstract BatchEventDao batchEventDao();

    public abstract SwapActionDao swapActionDao();

    public abstract PurchaseItemDao purchaseItemDao();

    /** Empties every table. Only for the sample-data file; see ReadyShelfRepository. */
    abstract SampleResetDao sampleResetDao();

    /**
     * Opens (or creates) the database in the file {@code fileName}: the household's file, the
     * sample-data file, or a test file. DataSets calls this once for each file. Every file gets
     * the same migrations, so its data is kept.
     */
    @NonNull
    static AppDatabase build(@NonNull Context context, @NonNull String fileName) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, fileName)
                .addCallback(new SeedLocations())
                .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3,
                        Migrations.MIGRATION_3_4)
                .build();
    }

    /**
     * True only for a sample-data file: its name ends with {@link #SAMPLE_FILE_ENDING}.
     * False for the household's file, and for a database in memory (it has no name).
     */
    static boolean isSampleDataFile(@NonNull AppDatabase database) {
        return isSampleDataFileName(database.getOpenHelper().getDatabaseName());
    }

    /** The name check of {@link #isSampleDataFile}. A database in memory has no name (null). */
    static boolean isSampleDataFileName(@Nullable String name) {
        return name != null && !name.equals(FILE_NAME) && name.endsWith(SAMPLE_FILE_ENDING);
    }

    /** Adds the three starting locations, only when the database file is first created. */
    private static final class SeedLocations extends RoomDatabase.Callback {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            for (StartingLocation location : STARTING_LOCATIONS) {
                // Room stores an enum as its name, for example "EMERGENCY_KIT".
                db.execSQL("INSERT INTO locations (name, type) VALUES (?, ?)",
                        new Object[] {location.name, location.type.name()});
            }
        }
    }
}

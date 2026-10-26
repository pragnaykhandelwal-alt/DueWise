package com.example.readyshelf.data;

import androidx.annotation.NonNull;

/**
 * One set of data that the screens can show: one database file, read and changed through its
 * repository, and the planning settings that go with it. There are two sets: the household's
 * own records and the separate sample data (see DataSets).
 */
public final class DataSet {

    @NonNull public final ReadyShelfRepository repository;
    @NonNull public final SettingsRepository settings;

    /** True for the separate sample data; false for the household's own records. */
    public final boolean sample;

    /** The database file itself, for tests. */
    @NonNull final AppDatabase database;

    DataSet(@NonNull AppDatabase database, @NonNull SettingsRepository settings, boolean sample) {
        this.database = database;
        this.repository = new ReadyShelfRepository(database);
        this.settings = settings;
        this.sample = sample;
    }
}

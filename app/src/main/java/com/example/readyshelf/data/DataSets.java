package com.example.readyshelf.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.example.readyshelf.data.ReadyShelfRepository.SaveListener;
import com.example.readyshelf.data.ReadyShelfRepository.SaveResult;
import com.example.readyshelf.domain.DateThresholds;

import java.time.LocalDate;

/**
 * The household's own records and the separate sample data, and which of the two the screens
 * show. AppContainer holds one object of this class.
 *
 * <p>How the household's records stay apart from the sample data:
 * <ul>
 *   <li>They are two files, each with its own planning settings: {@code readyshelf.db} and
 *       {@code readyshelf-sample-data.db}. Sample data is written to the household's file only
 *       when the household chooses to keep it as its own ({@link #keepSampleAsHousehold}).</li>
 *   <li>Only a sample-data file can be emptied (ReadyShelfRepository checks the file name).</li>
 *   <li>Each screen's ViewModel asks {@link #active()} once and keeps that set, and every switch
 *       makes all screens again (SampleDataClose), so no screen shows one set and saves into
 *       the other.</li>
 *   <li>The daily reminder always checks {@link #household()}.</li>
 * </ul>
 */
public final class DataSets {

    private static final String TAG = "DataSets";

    /** The sample data's own planning settings. */
    static final String SAMPLE_SETTINGS_FILE_NAME = "sample_settings";
    /** The small file that remembers whether sample data is open. */
    static final String MODE_FILE_NAME = "data_mode";
    private static final String KEY_SAMPLE_OPEN = "sample_data_open";

    private final Context context;
    private final String sampleFileName;
    private final String sampleSettingsFileName;
    private final SharedPreferences mode;
    private final DataSet household;
    @Nullable private DataSet sample;   // made the first time it is needed
    private volatile boolean sampleOpen;

    /** The app's own files. */
    public DataSets(@NonNull Context context) {
        this(context, AppDatabase.FILE_NAME, SettingsRepository.FILE_NAME,
                AppDatabase.SAMPLE_FILE_NAME, SAMPLE_SETTINGS_FILE_NAME, MODE_FILE_NAME);
    }

    /**
     * Files of a test's own.
     *
     * @throws IllegalArgumentException when {@code sampleFileName} is not a sample-data file name
     */
    @VisibleForTesting
    DataSets(@NonNull Context context, @NonNull String householdFileName,
             @NonNull String householdSettingsFileName, @NonNull String sampleFileName,
             @NonNull String sampleSettingsFileName, @NonNull String modeFileName) {
        if (!sampleFileName.endsWith(AppDatabase.SAMPLE_FILE_ENDING)
                || sampleFileName.equals(householdFileName)
                || sampleFileName.equals(AppDatabase.FILE_NAME)) {
            throw new IllegalArgumentException("Not a sample-data file name: " + sampleFileName);
        }
        this.context = context.getApplicationContext();
        this.sampleFileName = sampleFileName;
        this.sampleSettingsFileName = sampleSettingsFileName;
        // Room opens the file only when it is first used, on a background thread.
        household = new DataSet(AppDatabase.build(this.context, householdFileName),
                new SettingsRepository(this.context, householdSettingsFileName), false);
        mode = this.context.getSharedPreferences(modeFileName, Context.MODE_PRIVATE);
        sampleOpen = mode.getBoolean(KEY_SAMPLE_OPEN, false);
    }

    /** The household's own records. The daily reminder always uses these. */
    @NonNull
    public DataSet household() {
        return household;
    }

    /**
     * The set the screens show now: the sample data while it is open, otherwise the household's
     * records. A screen's ViewModel asks once, when it is made, and keeps the answer.
     */
    @NonNull
    public DataSet active() {
        return sampleOpen ? sample() : household;
    }

    /** True while sample data is open. Every screen then shows the "Sample data" label. */
    public boolean isSampleOpen() {
        return sampleOpen;
    }

    /**
     * Opening sample data, step 1: fills the sample-data file with fresh sample data dated from
     * {@code today}, on the sample data's own background thread. Nothing switches yet, and the
     * household's records are not touched. The listener hears SAVED or FAILED on that thread.
     */
    public void prepareSample(@NonNull LocalDate today, @NonNull String historyDetails,
                              @NonNull SaveListener listener) {
        sample().repository.replaceWithSampleData(today, historyDetails, listener);
    }

    /**
     * Opening sample data, step 2, after step 1 saved: the sample planning settings get their
     * starting values again, and the choice is saved, so the screens made from now on show the
     * sample data. The caller then makes all screens again. Returns false when the choice could
     * not be saved; then nothing switched.
     */
    @MainThread
    public boolean switchToSample() {
        DataSet sampleSet = sample();
        sampleSet.settings.saveThresholds(DateThresholds.defaults());
        if (!mode.edit().putBoolean(KEY_SAMPLE_OPEN, true).commit()) {
            return false;
        }
        sampleOpen = true;
        return true;
    }

    /**
     * Closes sample data: the choice is saved, so the screens made from now on show the
     * household's records again. The caller then makes all screens again. The sample-data file
     * is emptied afterwards on its own background thread; if that fails, the next opening starts
     * over anyway. Returns false when the choice could not be saved; then nothing switched.
     */
    @MainThread
    public boolean switchToHousehold() {
        return switchToHousehold(result -> { });
    }

    /**
     * Keeping the sample data as the household's own, step 1: copies everything in the sample
     * data into the household's records (see {@link ReadyShelfRepository#copyFromSampleData}).
     * After a successful copy the sample-data file is emptied right away, so the same items can
     * never be copied twice, even if the screen closes before step 2. Nothing switches yet: step
     * 2 is {@link #switchToHousehold()}. The listener hears the copy's result, and how many
     * batches were copied, on a background thread.
     */
    public void keepSampleAsHousehold(@NonNull String historyDetails,
                                      @NonNull ReadyShelfRepository.CopyListener listener) {
        DataSet sampleSet = sample();
        household.repository.copyFromSampleData(sampleSet.repository, historyDetails,
                (result, copiedBatches) -> {
                    if (result != SaveResult.SAVED) {
                        listener.onResult(result, 0);
                        return;
                    }
                    // The copies are saved in the household's file. Closing sample data empties
                    // the sample file again; if this first try fails, that one still runs.
                    sampleSet.repository.clearSampleData(emptied -> {
                        if (emptied != SaveResult.SAVED) {
                            Log.w(TAG, "The sample data was copied but could not be emptied yet.");
                        }
                        listener.onResult(SaveResult.SAVED, copiedBatches);
                    });
                });
    }

    /** {@link #switchToHousehold()}, and {@code emptied} hears whether the sample file was emptied. */
    @VisibleForTesting
    @MainThread
    boolean switchToHousehold(@NonNull SaveListener emptied) {
        if (!mode.edit().putBoolean(KEY_SAMPLE_OPEN, false).commit()) {
            return false;
        }
        sampleOpen = false;
        sample().repository.clearSampleData(result -> {
            if (result != SaveResult.SAVED) {
                Log.w(TAG, "The sample data could not be emptied. Opening it again starts over.");
            }
            emptied.onResult(result);
        });
        return true;
    }

    /** The separate sample data. Its file is opened the first time it is used. */
    @NonNull
    synchronized DataSet sample() {
        if (sample == null) {
            sample = new DataSet(AppDatabase.build(context, sampleFileName),
                    new SettingsRepository(context, sampleSettingsFileName), true);
        }
        return sample;
    }

    /** Closes both database files. Tests only: the app keeps them open while it runs. */
    @VisibleForTesting
    synchronized void close() {
        household.database.close();
        if (sample != null) {
            sample.database.close();
        }
    }
}

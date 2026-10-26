package com.example.readyshelf;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.data.DataSets;
import com.example.readyshelf.data.LabelPhotoReader;
import com.example.readyshelf.domain.DateCandidateParser;
import com.example.readyshelf.domain.DateCandidateParserContract;
import com.example.readyshelf.domain.DatePolicy;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.RotationPlanner;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.reminders.ReminderNotifier;
import com.example.readyshelf.reminders.ReminderPrefs;
import com.example.readyshelf.reminders.ReminderScheduler;
import com.example.readyshelf.reminders.Reminders;
import com.example.readyshelf.ui.common.Confirmations;
import com.example.readyshelf.widget.ReviewWidget;

/**
 * Holds the objects that the whole app shares. ReadyShelfApp makes one AppContainer
 * when the app starts, and the screens get it from there.
 */
public final class AppContainer {

    /**
     * The household's own records and the separate sample data. Each ViewModel asks
     * {@code dataSets.active()} once, when it is made, and uses that set for its whole life.
     */
    @NonNull public final DataSets dataSets;
    /** Reads the text on label photos, on the phone, on its own background thread. */
    @NonNull public final LabelPhotoReader labelPhotoReader;

    /**
     * The date rules (DatePolicy), connected on 2026-10-18. If this were null, every feature that
     * needs date rules would stay turned off, the daily reminder too.
     */
    @Nullable public final DatePolicyContract datePolicy;

    /**
     * The swap rules (RotationPlanner), connected on 2026-10-18. If this were null, Home would show
     * no swaps, and the Purchase list's replacement needs and suggestions would stay turned off.
     */
    @Nullable public final RotationPlannerContract rotationPlanner;

    /**
     * The date-candidate rules (DateCandidateParser), connected on 2026-10-18. If this were null,
     * photo entry would show the text it read but no possible dates.
     */
    @Nullable public final DateCandidateParserContract dateCandidateParser;

    /** The daily reminder: its switch, schedule, daily check, and notifications. */
    @NonNull public final Reminders reminders;

    /** The home-screen widget "Review list": what it says, and when it is refreshed. */
    @NonNull public final ReviewWidget reviewWidget;

    /**
     * A save message that waits for the next screen, when the screen that saved closes right
     * away (Confirmations).
     */
    @NonNull public final Confirmations.Waiting confirmations = new Confirmations.Waiting();

    AppContainer(@NonNull Context context) {
        dataSets = new DataSets(context);
        labelPhotoReader = new LabelPhotoReader(context);

        // DatePolicy was written, tested, and connected on 2026-10-18. It holds no data that
        // changes, so the screens and the daily reminder check share this one object.
        datePolicy = new DatePolicy();
        // RotationPlanner was written, tested, and connected on 2026-10-18. It holds no data that
        // changes, so the screens and the swap save share this one object.
        rotationPlanner = new RotationPlanner();
        // DateCandidateParser was written, tested, and connected on 2026-10-18. It holds no data
        // that changes, so photo entry shares this one object.
        dateCandidateParser = new DateCandidateParser();

        // Made after datePolicy, so connecting the date rules above also turns on reminders.
        // The daily reminder always checks the household's own records, never the sample data.
        // Saved changes to those records keep a shown reminder (and its number on the app icon)
        // up to date.
        DataSet household = dataSets.household();
        reminders = new Reminders(new ReminderPrefs(context, ReminderPrefs.FILE_NAME),
                new ReminderNotifier(context), new ReminderScheduler(context),
                household.repository::activeBatchesNow, household.settings::current,
                household.repository::listenForBatchChanges, datePolicy);
        // Like the daily reminder, the home-screen widget shows only the household's own records.
        reviewWidget = new ReviewWidget(context, household.repository::activeBatchesNow,
                household.settings::current, household.repository::listenForBatchChanges,
                datePolicy);
    }
}

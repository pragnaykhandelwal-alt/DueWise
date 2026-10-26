package com.example.readyshelf;

import android.app.Application;

import androidx.annotation.NonNull;

import com.example.readyshelf.ui.common.Confirmations;

/**
 * Runs once when the app starts, before any screen opens. It makes the AppContainer.
 * AndroidManifest.xml names this class in android:name.
 */
public class ReadyShelfApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        // The reminder channel, and exactly one daily check while reminders are on. After a
        // force-stop, this is also when WorkManager schedules the check again.
        container.reminders.onAppStart();
        // The home-screen widget, if one is placed: its words, and hearing about later changes.
        container.reviewWidget.refreshSoon();
        // A save message left by a screen that closed shows on the next screen (Confirmations).
        registerActivityLifecycleCallbacks(
                Confirmations.showWaitingOnResume(container.confirmations));
    }

    /** The shared objects. ViewModels call this with getApplication(). */
    @NonNull
    public static AppContainer containerOf(@NonNull Application application) {
        return ((ReadyShelfApp) application).container;
    }
}

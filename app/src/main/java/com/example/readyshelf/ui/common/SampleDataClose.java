package com.example.readyshelf.ui.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.ui.home.HomeActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Closing the separate sample data, from "Close sample data" under the "Sample data" label of
 * every screen (ScreenSetup). A short question comes first, because the made-up items and the
 * changes made to them are deleted. Then every screen is made again, so Home opens with the
 * household's own records. Since 2026-10-22 Settings has no Sample data card, so sample data
 * can't be opened again; this only closes sample data that was already open.
 */
public final class SampleDataClose {

    private SampleDataClose() {
        // Only static methods, so no objects of this class are made.
    }

    /** Asks "Close sample data?"; its "Close" closes it. The question closes with its screen. */
    public static void confirm(@NonNull AppCompatActivity activity) {
        Dialog question = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.settings_close_sample_title)
                .setMessage(R.string.settings_close_sample_message)
                .setPositiveButton(R.string.sample_data_close_confirm,
                        (dialog, which) -> close(activity))
                .setNegativeButton(R.string.cancel, null)
                .show();
        // Closed in onDestroy, so it can't leak when the screen rotates.
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                question.dismiss();
            }
        });
    }

    /**
     * Closes sample data: the screens made from now on show the household's records again (see
     * DataSets), and every screen is made again. If the choice could not be saved, nothing
     * switched, and a message says so.
     */
    private static void close(@NonNull Activity activity) {
        if (!ReadyShelfApp.containerOf(activity.getApplication()).dataSets.switchToHousehold()) {
            Confirmations.show(activity, activity.getString(R.string.settings_sample_close_failed));
            return;
        }
        restartAtHome(activity, activity.getString(R.string.settings_sample_closed));
    }

    /**
     * After the switch back to the household's records: closes every screen and opens Home again,
     * so every screen shows the household's records without the label (see ScreenSetup). The new
     * Home shows {@code message} (Confirmations). The task then starts the same way as from the
     * launcher icon, so a later tap on the icon brings it back as it is.
     */
    private static void restartAtHome(@NonNull Activity activity, @NonNull CharSequence message) {
        Confirmations.showOnNextScreen(activity, message);
        activity.startActivity(Intent.makeRestartActivityTask(
                new ComponentName(activity, HomeActivity.class)));
    }
}

package com.example.readyshelf.ui.common;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.google.android.material.snackbar.Snackbar;

/**
 * Short messages after a save, such as "Batch added." or "Swap recorded.", at the bottom of the
 * screen (a Material Snackbar). TalkBack reads them aloud, and Android keeps them on screen longer
 * for people who use it. On a screen with the floating "Add batch" button, the message appears
 * above the button; on a screen with a bar of buttons at the bottom, above the bar.
 *
 * <p>A screen that closes right after saving leaves its message for the next DueWise screen that
 * comes to the front, so the household always sees what was saved. ReadyShelfApp registers
 * {@link #showWaitingOnResume} for that. The waiting message is kept in memory only.
 */
public final class Confirmations {

    /** Lines a message may use, so a long one is still shown in full with large text. */
    private static final int MAX_LINES = 4;

    /** The one message that waits for the next screen. AppContainer keeps the only object. */
    public static final class Waiting {
        @Nullable private CharSequence message;
    }

    private Confirmations() {
        // Only static methods, so no objects of this class are made.
    }

    /** Shows {@code message} on this screen now. */
    public static void show(@NonNull Activity activity, @NonNull CharSequence message) {
        View root = activity.findViewById(R.id.main);
        if (root == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG);
        snackbar.setTextMaxLines(MAX_LINES);
        // Above the floating "Add batch" button, or above the bar with a screen's main button
        // (for example on the new empty form after "Save and add another"), so it covers neither.
        View anchor = shown(activity.findViewById(R.id.fab_add_batch));
        if (anchor == null) {
            anchor = shown(activity.findViewById(R.id.bottom_actions));
        }
        if (anchor != null) {
            snackbar.setAnchorView(anchor);
        }
        snackbar.show();
    }

    /** {@code view} if the screen has it and it is shown, otherwise null. */
    @Nullable
    private static View shown(@Nullable View view) {
        return view != null && view.getVisibility() == View.VISIBLE ? view : null;
    }

    /** For a screen that closes right after saving: the next DueWise screen shows the message. */
    public static void showOnNextScreen(@NonNull Activity activity, @NonNull CharSequence message) {
        ReadyShelfApp.containerOf(activity.getApplication()).confirmations.message = message;
    }

    /**
     * Shows the waiting message, if there is one, once a screen has come to the front. Only the
     * moment a screen comes to the front matters here; the others do nothing.
     */
    @NonNull
    public static Application.ActivityLifecycleCallbacks showWaitingOnResume(
            @NonNull Waiting waiting) {
        return new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                CharSequence message = waiting.message;
                View root = activity.findViewById(R.id.main);
                if (message != null && root != null) {
                    waiting.message = null;
                    // After the screen is laid out, so the message slides in where it belongs.
                    root.post(() -> show(activity, message));
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity,
                                          @Nullable Bundle savedInstanceState) {
                // Not needed.
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                // Not needed.
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Not needed.
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                // Not needed.
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity,
                                                    @NonNull Bundle outState) {
                // Not needed.
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                // Not needed.
            }
        };
    }
}

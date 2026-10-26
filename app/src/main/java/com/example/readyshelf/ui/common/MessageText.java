package com.example.readyshelf.ui.common;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.readyshelf.R;

/** Shows the message line of a screen section, such as "Nothing needs review right now.". */
public final class MessageText {

    private MessageText() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Shows that the rules stopped with an error, in the error look, if they did; otherwise the
     * message if there is one, in the note look (NoticeLook). Hides the view when there is
     * nothing to say. The error's details are not shown: words like "java.lang.…Exception" mean
     * nothing to a household. The ViewModels write them to the phone's log instead.
     *
     * @param message    a string resource, or 0 for "no message"
     * @param rulesError the error from the student's rules, or null
     */
    public static void show(@NonNull TextView view, @StringRes int message,
                            @Nullable String rulesError) {
        if (rulesError != null) {
            view.setText(R.string.rules_error);
            NoticeLook.error(view);
            view.setVisibility(View.VISIBLE);
        } else if (message != 0) {
            view.setText(message);
            NoticeLook.note(view);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}

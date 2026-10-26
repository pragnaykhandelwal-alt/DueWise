package com.example.readyshelf.ui.settings;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.Confirmations;
import com.example.readyshelf.ui.common.MoreText;
import com.example.readyshelf.ui.common.NoticeLook;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.settings.SettingsViewModel.PendingAction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;

/**
 * Reminders and planning settings. Until 2026-10-22 it also had a Sample data card; the student
 * removed it. While sample data is open, "Close sample data" under the label of every screen
 * closes it (SampleDataClose).
 */
public class SettingsActivity extends AppCompatActivity {

    // Asks Android for the notification permission (Android 13 and newer). It is registered
    // every time the screen is made (Android requires that), so the answer still arrives after
    // the screen was made again.
    private final ActivityResultLauncher<String> askNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    this::onPermissionAnswer);

    private SettingsViewModel viewModel;
    private TextInputLayout nearDateLayout;
    private EditText nearDateField;
    private TextInputLayout replacementLayout;
    private EditText replacementField;
    private MaterialSwitch reminderSwitch;
    private TextView remindersOffNote;
    private TextView notificationsStatus;
    private TextView lastReminder;
    // A dialog that is open now. It is closed in onDestroy, so it can't leak when the screen rotates.
    @Nullable private Dialog openDialog;
    // While a planning number is changed but not saved, Back asks first (confirmLeave).
    private OnBackPressedCallback askBeforeLeaving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_settings);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Off until a planning number is changed (updateLeaveQuestion). The back arrow goes the
        // same way as the phone's Back, so both ask first.
        askBeforeLeaving = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, askBeforeLeaving);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        nearDateLayout = findViewById(R.id.settings_near_date_days_layout);
        nearDateField = findViewById(R.id.settings_near_date_days);
        replacementLayout = findViewById(R.id.settings_replacement_days_layout);
        replacementField = findViewById(R.id.settings_replacement_days);
        reminderSwitch = findViewById(R.id.settings_reminders);
        remindersOffNote = findViewById(R.id.settings_reminders_off);
        notificationsStatus = findViewById(R.id.settings_notifications_status);
        lastReminder = findViewById(R.id.settings_last_reminder);

        MoreText.setUp(findViewById(R.id.settings_reminders_more_toggle),
                findViewById(R.id.settings_reminders_more), savedInstanceState);

        findViewById(R.id.settings_thresholds_note)
                .setVisibility(viewModel.hasAllRules() ? View.GONE : View.VISIBLE);
        findViewById(R.id.settings_thresholds_sample_note)
                .setVisibility(viewModel.isSampleDataOpen() ? View.VISIBLE : View.GONE);

        viewModel.getThresholds().observe(this, thresholds -> {
            if (viewModel.shouldFillThresholdFields()) {
                nearDateField.setText(String.valueOf(thresholds.getNearDateDays()));
                replacementField.setText(String.valueOf(thresholds.getReplacementDays()));
            }
            // After the first fill, and after each save, the saved numbers are new.
            updateLeaveQuestion();
        });
        findViewById(R.id.settings_save_thresholds).setOnClickListener(view -> saveThresholds());
        // The keyboard's "Done" key on the second number saves, like the button.
        replacementField.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveThresholds();
                return true;
            }
            return false;
        });
        // Also called when the fields are filled, and when a rotation puts the typing back.
        TextWatcher typing = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // Not needed.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // Not needed.
            }

            @Override
            public void afterTextChanged(Editable text) {
                checkThresholds(false);
                updateLeaveQuestion();
            }
        };
        nearDateField.addTextChangedListener(typing);
        replacementField.addTextChangedListener(typing);

        // A click (not a checked-change listener): only the household's tap turns reminders
        // on or off. The switch then shows the saved choice again.
        reminderSwitch.setOnClickListener(view -> onReminderSwitchTapped());
        findViewById(R.id.settings_show_reminder).setOnClickListener(
                view -> whenNotificationsAllowed(PendingAction.SHOW_NOW));
        findViewById(R.id.settings_open_notification_settings).setOnClickListener(
                view -> openNotificationSettings());
        // What "Show reminder now" did. It arrives a moment after the tap, also after a rotation.
        viewModel.getMessage().observe(this, message -> {
            if (message != null) {
                showMessage(message);
                viewModel.messageShown();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The household may have changed ReadyShelf's notifications in Android's settings.
        showReminders();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Whether "More about this" is open, so a rotation keeps it.
        MoreText.save(outState, findViewById(R.id.settings_reminders_more));
    }

    @Override
    protected void onDestroy() {
        if (openDialog != null) {
            openDialog.dismiss();
        }
        super.onDestroy();
    }

    // ----- Planning settings -----

    /** Checks both numbers. If they are fine, saves them, closes the keyboard, and says so. */
    private void saveThresholds() {
        DateThresholds thresholds = checkThresholds(true);
        if (thresholds == null) {
            return;
        }
        viewModel.saveThresholds(thresholds);
        hideKeyboard();
        showMessage(R.string.settings_thresholds_saved);
    }

    /**
     * Checks both numbers: the near-date days from 1 to 365, and the replacement days from 1 to
     * 3650 and more than the near-date days. Returns them when both are fine, otherwise null.
     *
     * <p>On Save ({@code onSave} true), each field shows its error or none, and the first field
     * with an error gets the focus. While the household types, an error can only go away: when
     * the number is fixed, or when the error no longer fits it (for example "more than the
     * near-date days (200)" after the 200 changed). The next Save shows the error that fits. So
     * no error appears halfway through typing, and TalkBack doesn't read errors over and over.
     */
    @Nullable
    private DateThresholds checkThresholds(boolean onSave) {
        Integer nearDateDays = parseDays(nearDateField);
        Integer replacementDays = parseDays(replacementField);
        boolean nearDateOk = nearDateDays != null && DateThresholds.isValidNearDateDays(nearDateDays);
        boolean replacementOk = replacementDays != null
                && DateThresholds.isValidReplacementDays(replacementDays);
        // The replacement days must be more than the near-date days.
        boolean pairOk = !nearDateOk || !replacementOk
                || DateThresholds.isValidPair(nearDateDays, replacementDays);
        String nearDateError = nearDateOk ? null : getString(R.string.error_near_date_days);
        String replacementError = null;
        if (!replacementOk) {
            replacementError = getString(R.string.error_replacement_days);
        } else if (!pairOk) {
            replacementError = getString(R.string.error_replacement_not_more, nearDateDays);
        }
        showError(nearDateLayout, nearDateError, onSave);
        showError(replacementLayout, replacementError, onSave);
        if (!nearDateOk) {
            if (onSave) {
                nearDateField.requestFocus();
            }
            return null;
        }
        if (replacementError != null) {
            if (onSave) {
                replacementField.requestFocus();
            }
            return null;
        }
        return DateThresholds.of(nearDateDays, replacementDays);
    }

    /** On Save, shows {@code error}, or no error when it is null. While typing, only clears. */
    private static void showError(@NonNull TextInputLayout layout, @Nullable String error,
                                  boolean onSave) {
        if (onSave) {
            layout.setError(error);
        } else if (layout.getError() != null && !TextUtils.equals(layout.getError(), error)) {
            layout.setError(null);
        }
    }

    /** Back asks first (confirmLeave) only while a field differs from the saved numbers. */
    private void updateLeaveQuestion() {
        askBeforeLeaving.setEnabled(hasUnsavedNumbers());
    }

    /** True when a field holds another number than the saved one, or no whole number at all. */
    private boolean hasUnsavedNumbers() {
        DateThresholds saved = viewModel.getThresholds().getValue();
        if (saved == null) {
            return false;
        }
        Integer nearDateDays = parseDays(nearDateField);
        Integer replacementDays = parseDays(replacementField);
        // null is checked first: comparing a null Integer with an int would throw.
        return nearDateDays == null || replacementDays == null
                || nearDateDays != saved.getNearDateDays()
                || replacementDays != saved.getReplacementDays();
    }

    /**
     * Back while a planning number is changed but not saved: asks first, so a change isn't lost
     * by accident. "Leave" closes Settings and saves nothing; "Keep editing" stays here.
     */
    private void confirmLeave() {
        openDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_leave_title)
                .setMessage(R.string.settings_leave_message)
                .setPositiveButton(R.string.settings_leave_confirm, (dialog, which) -> finish())
                .setNegativeButton(R.string.settings_leave_keep_editing, null)
                .show();
    }

    /** Closes the keyboard, so the message at the bottom of the screen is in view. */
    private void hideKeyboard() {
        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(nearDateField.getWindowToken(), 0);
        }
    }

    /** The whole number in a field, or null when it isn't one. */
    @Nullable
    private static Integer parseDays(EditText field) {
        String text = field.getText() == null ? "" : field.getText().toString().trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    // ----- Reminders -----

    private void onReminderSwitchTapped() {
        if (viewModel.areRemindersOn()) {
            viewModel.turnRemindersOff();
            showMessage(R.string.settings_reminder_turned_off);
        } else {
            whenNotificationsAllowed(PendingAction.ENABLE);
        }
        showReminders();
    }

    /**
     * Does {@code action} when notifications are allowed. Otherwise, on Android 13 and newer,
     * asks for the permission first. When Android can't ask (notifications are turned off in
     * its settings, or the household said "Don't allow" before), it explains instead.
     */
    private void whenNotificationsAllowed(@NonNull PendingAction action) {
        if (viewModel.canShowNotifications()) {
            complete(action);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            viewModel.setPendingAction(action);
            askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            showMessage(R.string.settings_notifications_refused);
        }
        showReminders();
    }

    /**
     * Android's answer. After the household said "Don't allow" twice, Android answers "no" at
     * once, without asking; the message and the settings button cover that too.
     */
    private void onPermissionAnswer(Boolean granted) {
        PendingAction action = viewModel.takePendingAction();
        if (Boolean.TRUE.equals(granted) && viewModel.canShowNotifications()) {
            complete(action);
        } else if (action != PendingAction.NONE) {
            showMessage(R.string.settings_notifications_refused);
        }
        showReminders();
    }

    private void complete(@NonNull PendingAction action) {
        if (action == PendingAction.ENABLE) {
            if (viewModel.turnRemindersOn()) {
                showMessage(R.string.settings_reminder_turned_on);
            }
        } else if (action == PendingAction.SHOW_NOW) {
            viewModel.showReminderNow();
        }
    }

    /**
     * Shows the saved reminder choice, whether notifications are allowed, and the last reminder.
     * The notification box is a warning only while the daily reminder is on but can't appear.
     * Otherwise it is a quiet note, so a household that keeps notifications off isn't pressed to
     * change that (Android's guidance for a refused permission).
     */
    private void showReminders() {
        boolean available = viewModel.areRemindersAvailable();
        boolean on = available && viewModel.areRemindersOn();
        boolean allowed = viewModel.canShowNotifications();
        reminderSwitch.setEnabled(available);
        reminderSwitch.setChecked(on);
        remindersOffNote.setVisibility(available ? View.GONE : View.VISIBLE);
        notificationsStatus.setText(allowed
                ? R.string.settings_notifications_allowed
                : R.string.settings_notifications_not_allowed);
        if (on && !allowed) {
            NoticeLook.warning(notificationsStatus);
        } else {
            NoticeLook.note(notificationsStatus);
        }
        LocalDate last = viewModel.lastReminderDay();
        lastReminder.setText(last == null ? getString(R.string.settings_last_reminder_none)
                : getString(R.string.settings_last_reminder, BatchText.date(last)));
    }

    /**
     * Opens Android's notification settings for ReadyShelf. If a phone doesn't have that page,
     * it opens ReadyShelf's app page instead, and as a last resort explains the way there.
     */
    private void openNotificationSettings() {
        Intent notificationSettings = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(notificationSettings);
            return;
        } catch (ActivityNotFoundException noSuchPage) {
            // Try the app page below.
        }
        Intent appPage = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        try {
            startActivity(appPage);
        } catch (ActivityNotFoundException noSuchPage) {
            // A dialog, not a short message at the bottom: that disappears after a few seconds,
            // and the way there must stay readable in full.
            openDialog = new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.settings_notification_settings_missing)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    /** A short message at the bottom of the screen (Confirmations). */
    private void showMessage(@StringRes int message) {
        Confirmations.show(this, getString(message));
    }
}

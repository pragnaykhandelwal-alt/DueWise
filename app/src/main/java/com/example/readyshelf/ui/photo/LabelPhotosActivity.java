package com.example.readyshelf.ui.photo;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.TorchState;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TapToFocusInfo;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.PhotoDateChoices;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.photo.LabelPhotosViewModel.Notice;
import com.example.readyshelf.ui.photo.LabelPhotosViewModel.Photo;
import com.example.readyshelf.ui.photo.LabelPhotosViewModel.PhotoState;
import com.example.readyshelf.ui.photo.LabelPhotosViewModel.State;
import com.example.readyshelf.ui.photo.LabelPhotosViewModel.Step;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * The Label photos screen (SPEC.md, "Photo entry"): a photo of the product and a close photo of
 * the printed date, taken with ReadyShelf's own camera (CameraX) or chosen from saved photos.
 * The text is read on the phone and shown for review, with the possible dates. The household
 * taps one date (or none) and goes back to the form. Nothing is saved on this screen.
 *
 * <p>The camera permission is asked for only here. Without it, the household can still choose a
 * saved photo, skip a photo, or go back and fill in the form by hand.
 */
public class LabelPhotosActivity extends AppCompatActivity {

    private static final String EXTRA_RESULT = "com.example.readyshelf.PHOTO_ENTRY_RESULT";

    // How long "Take photo" waits for the camera to focus. On the household's phone a tap to
    // focus took 0.1 to 1.9 seconds (2026-10-18).
    private static final long FOCUS_WAIT_SECONDS = 4;

    /** Opens the screen. Its result is what the household chose, or null when they cancelled. */
    public static final class Contract extends ActivityResultContract<Void, PhotoEntryResult> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @Nullable Void input) {
            return new Intent(context, LabelPhotosActivity.class);
        }

        @Nullable
        @Override
        public PhotoEntryResult parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return null;
            }
            return IntentCompat.getSerializableExtra(intent, EXTRA_RESULT, PhotoEntryResult.class);
        }
    }

    // Registered every time the screen is made (Android requires that), so an answer still
    // arrives after the screen rotated or Android closed and reopened the app.
    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> cameraAnswered());
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onPhotoPicked);

    private LabelPhotosViewModel viewModel;
    private LifecycleCameraController camera;
    private Executor mainThread;
    private boolean hasCamera;      // the phone has a camera that ReadyShelf can use
    private boolean cameraFailed;   // CameraX could not start
    private boolean cameraBound;    // the camera is on
    private boolean askingNow;      // Android's permission question is on screen
    private boolean focusingForPhoto; // "Take photo" is waiting for the camera to focus
    @Nullable private Photo notFocusedFor; // the step whose last "Take photo" could not focus
    // The choices whose buttons are on screen, so the buttons are only made again for new choices.
    @Nullable private PhotoDateChoices shownChoices;
    // A dialog that is open now. It is closed in onDestroy, so it can't leak when the screen rotates.
    @Nullable private AlertDialog openDialog;

    // A photo step
    private View captureGroup;
    private View previewFrame;
    private PreviewView preview;
    private TextView stepTitle;
    private TextView stepHint;
    private TextView cameraMessage;
    private TextView cameraStatus;
    private Button takeButton;
    private Button lightButton;
    private Button askAgainButton;
    private Button settingsButton;
    private Button chooseButton;
    private Button skipButton;
    // The review
    private View reviewScroll;
    private TextView reviewStatus;
    // A moving bar while the photos are read.
    private View reviewProgress;
    private TextView productText;
    private TextView productNote;
    private TextView dateText;
    private TextView datesMessage;
    private LinearLayout dateChoices;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_label_photos);
        viewModel = new ViewModelProvider(this).get(LabelPhotosViewModel.class);
        // The application's executor, so a photo that finishes later never holds this screen.
        mainThread = ContextCompat.getMainExecutor(getApplicationContext());
        findViews();

        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        camera = new LifecycleCameraController(this);
        // Only the preview and taking photos: no image analysis runs.
        camera.setEnabledUseCases(CameraController.IMAGE_CAPTURE);
        // A sharp photo matters more here than a fast shutter. The flash stays off (the default),
        // because it makes glare on shiny packages; the household can turn on the light instead.
        camera.setImageCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
        // The preview handles tap to focus and pinch to zoom. The controller also turns each
        // photo to match how the phone is held (an EXIF tag that the text reader applies).
        preview.setController(camera);
        camera.getTapToFocusInfoState().observe(this, info -> {
            notFocusedFor = null; // a tap starts a new focus, with its own message
            showCameraStatus();
        });
        camera.getTorchState().observe(this, torch -> showLight());
        camera.getInitializationFuture().addListener(this::cameraReady, mainThread);

        takeButton.setOnClickListener(view -> takePhoto());
        lightButton.setOnClickListener(view -> toggleLight());
        askAgainButton.setOnClickListener(view -> askForCamera());
        settingsButton.setOnClickListener(view -> openAppSettings());
        chooseButton.setOnClickListener(view -> choosePhoto());
        skipButton.setOnClickListener(view -> skipPhoto());
        findViewById(R.id.photo_back_to_form).setOnClickListener(view -> finish());
        findViewById(R.id.photo_retake_product).setOnClickListener(view -> viewModel.retake(Photo.PRODUCT));
        findViewById(R.id.photo_retake_date).setOnClickListener(view -> viewModel.retake(Photo.DATE));
        continueButton.setOnClickListener(view -> finishWith(null));
        findViewById(R.id.photo_cancel).setOnClickListener(view -> finish());

        viewModel.getState().observe(this, this::show);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Coming back from Android's settings: the camera may be allowed now.
        updateCamera();
    }

    @Override
    protected void onDestroy() {
        if (openDialog != null) {
            openDialog.dismiss();
        }
        super.onDestroy();
    }

    private void findViews() {
        captureGroup = findViewById(R.id.photo_capture_group);
        previewFrame = findViewById(R.id.photo_preview_frame);
        preview = findViewById(R.id.photo_preview);
        stepTitle = findViewById(R.id.photo_step_title);
        stepHint = findViewById(R.id.photo_step_hint);
        cameraMessage = findViewById(R.id.photo_camera_message);
        cameraStatus = findViewById(R.id.photo_status);
        takeButton = findViewById(R.id.photo_take);
        lightButton = findViewById(R.id.photo_light);
        askAgainButton = findViewById(R.id.photo_ask_again);
        settingsButton = findViewById(R.id.photo_open_settings);
        chooseButton = findViewById(R.id.photo_choose);
        skipButton = findViewById(R.id.photo_skip);
        reviewScroll = findViewById(R.id.photo_review_scroll);
        reviewStatus = findViewById(R.id.photo_review_status);
        reviewProgress = findViewById(R.id.photo_review_progress);
        productText = findViewById(R.id.photo_product_text);
        productNote = findViewById(R.id.photo_product_note);
        dateText = findViewById(R.id.photo_date_text);
        datesMessage = findViewById(R.id.photo_dates_message);
        dateChoices = findViewById(R.id.photo_date_choices);
        continueButton = findViewById(R.id.photo_continue_without_date);
    }

    // ----- The camera -----

    /** CameraX has started, or failed to. It uses the back camera, or else the front camera. */
    private void cameraReady() {
        try {
            camera.getInitializationFuture().get(); // done already, so this doesn't wait
            if (!camera.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                if (camera.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    camera.setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA);
                } else {
                    hasCamera = false;
                }
            }
        } catch (ExecutionException | RuntimeException notStarted) {
            cameraFailed = true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            cameraFailed = true;
        }
        updateCamera();
    }

    /**
     * The camera is on only while a photo step is on screen and the camera is allowed. It is off
     * in the review, so Android's camera indicator goes away. Also shows the matching part of the
     * screen, and asks for the camera once by itself.
     */
    private void updateCamera() {
        State state = viewModel.getState().getValue();
        boolean photoStep = state != null && state.step != Step.REVIEW;
        boolean usable = hasCamera && !cameraFailed;
        boolean allowed = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (photoStep && usable && allowed) {
            if (!cameraBound) {
                camera.bindToLifecycle(this);
                cameraBound = true;
            }
        } else if (cameraBound) {
            camera.unbind();
            cameraBound = false;
        }
        if (photoStep && usable && !allowed && !askingNow && viewModel.shouldAskForCamera()) {
            askForCamera();
        }
        showCameraPart(photoStep, usable, allowed);
    }

    private void askForCamera() {
        viewModel.askedForCamera();
        askingNow = true;
        cameraPermission.launch(Manifest.permission.CAMERA);
    }

    private void cameraAnswered() {
        askingNow = false;
        updateCamera();
    }

    /** The preview and the shutter, or a message that explains what can be done instead. */
    private void showCameraPart(boolean photoStep, boolean usable, boolean allowed) {
        boolean cameraOn = photoStep && usable && allowed;
        previewFrame.setVisibility(cameraOn ? View.VISIBLE : View.GONE);
        takeButton.setVisibility(cameraOn ? View.VISIBLE : View.GONE);

        String message = null;
        boolean canAskAgain = false;
        boolean refused = false;
        if (photoStep) {
            if (!hasCamera) {
                message = getString(R.string.photo_no_camera);
            } else if (cameraFailed) {
                message = getString(R.string.photo_camera_failed);
            } else if (!allowed && askingNow) {
                message = getString(R.string.photo_camera_asking);
            } else if (!allowed) {
                refused = true;
                // True after one refusal; false when Android won't show its question again.
                canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
                message = getString(R.string.photo_camera_refused);
                if (!canAskAgain) {
                    message += " " + getString(R.string.photo_camera_refused_settings);
                }
            }
        }
        cameraMessage.setText(message);
        cameraMessage.setVisibility(message == null ? View.GONE : View.VISIBLE);
        askAgainButton.setVisibility(canAskAgain ? View.VISIBLE : View.GONE);
        settingsButton.setVisibility(refused ? View.VISIBLE : View.GONE);
        showLight();
        showCameraStatus();
    }

    /** "Turn on the light", shown only when the camera has a flash unit. */
    private void showLight() {
        CameraInfo info = cameraBound ? camera.getCameraInfo() : null;
        boolean hasLight = info != null && info.hasFlashUnit();
        lightButton.setVisibility(hasLight ? View.VISIBLE : View.GONE);
        lightButton.setText(isLightOn() ? R.string.photo_light_off : R.string.photo_light_on);
    }

    private boolean isLightOn() {
        Integer torch = camera.getTorchState().getValue();
        return torch != null && torch == TorchState.ON;
    }

    private void toggleLight() {
        camera.enableTorch(!isLightOn()); // the button changes when the torch state changes
    }

    /**
     * One status line: "Hold still…" while saving, "Focusing…" before a photo, a photo that
     * couldn't be taken because the camera couldn't focus, a notice about the last photo, or the
     * focus after a tap.
     */
    private void showCameraStatus() {
        State state = viewModel.getState().getValue();
        if (state == null || state.step == Step.REVIEW) {
            return;
        }
        Photo stepPhoto = state.step == Step.PRODUCT ? Photo.PRODUCT : Photo.DATE;
        PhotoState current = state.step == Step.PRODUCT ? state.productState : state.dateState;
        String text;
        if (current == PhotoState.SAVING) {
            text = getString(R.string.photo_hold_still);
        } else if (focusingForPhoto) {
            text = getString(R.string.photo_focusing);
        } else if (notFocusedFor == stepPhoto) {
            text = getString(R.string.photo_not_focused_no_photo);
        } else if (state.notice != Notice.NONE) {
            text = noticeText(state.notice);
        } else if (cameraBound) {
            text = focusText(camera.getTapToFocusInfoState().getValue());
        } else {
            text = "";
        }
        cameraStatus.setText(text);
        cameraStatus.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @NonNull
    private String focusText(@Nullable TapToFocusInfo info) {
        int focus = info == null ? CameraController.TAP_TO_FOCUS_NOT_STARTED : info.getFocusState();
        if (focus == CameraController.TAP_TO_FOCUS_STARTED) {
            return getString(R.string.photo_focusing);
        } else if (focus == CameraController.TAP_TO_FOCUS_FOCUSED) {
            return getString(R.string.photo_in_focus);
        } else if (focus == CameraController.TAP_TO_FOCUS_NOT_FOCUSED
                || focus == CameraController.TAP_TO_FOCUS_FAILED) {
            return getString(R.string.photo_not_focused);
        }
        return "";
    }

    @NonNull
    private String noticeText(@NonNull Notice notice) {
        if (notice == Notice.NOT_SAVED) {
            return getString(R.string.photo_not_saved);
        } else if (notice == Notice.LOST) {
            return getString(R.string.photo_lost);
        }
        return "";
    }

    // ----- Taking, choosing, and skipping photos -----

    /** The photo of the step on screen. */
    @Nullable
    private Photo currentPhoto() {
        State state = viewModel.getState().getValue();
        if (state == null || state.step == Step.REVIEW) {
            return null;
        }
        return state.step == Step.PRODUCT ? Photo.PRODUCT : Photo.DATE;
    }

    /**
     * "Take photo". The camera focuses first, on the point last tapped or else the middle of the
     * screen, and the photo is taken only when the camera says it is in focus. Without this, the
     * photo could be taken while the camera was still focusing: on the household's phone, whose
     * camera can't focus closer than 10 cm, both photos came out blurry and the text reader found
     * almost nothing (2026-10-18). A camera without autofocus takes the photo right away.
     */
    private void takePhoto() {
        Photo photo = currentPhoto();
        if (photo == null || !cameraBound || focusingForPhoto) {
            return;
        }
        notFocusedFor = null;
        CameraControl control = camera.getCameraControl();
        CameraInfo info = camera.getCameraInfo();
        FocusMeteringAction focus = new FocusMeteringAction.Builder(focusPoint(),
                FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(FOCUS_WAIT_SECONDS, TimeUnit.SECONDS)
                .build();
        if (control == null || info == null || !info.isFocusMeteringSupported(focus)) {
            capture(photo);
            return;
        }
        focusingForPhoto = true;
        showCameraStatus();
        ListenableFuture<FocusMeteringResult> result = control.startFocusAndMetering(focus);
        result.addListener(() -> focusDone(photo, result), mainThread);
    }

    /** The point last tapped to focus, or else the middle of the camera view. */
    @NonNull
    private MeteringPoint focusPoint() {
        TapToFocusInfo tap = camera.getTapToFocusInfoState().getValue();
        PointF tapped = tap == null ? null : tap.getTapPoint();
        float x = tapped != null ? tapped.x : preview.getWidth() / 2f;
        float y = tapped != null ? tapped.y : preview.getHeight() / 2f;
        return preview.getMeteringPointFactory().createPoint(x, y);
    }

    /** The camera has focused, or couldn't (it waits at most FOCUS_WAIT_SECONDS). */
    private void focusDone(@NonNull Photo photo, @NonNull ListenableFuture<FocusMeteringResult> result) {
        focusingForPhoto = false;
        boolean focused;
        try {
            focused = result.get().isFocusSuccessful(); // done already, so this doesn't wait
        } catch (ExecutionException | RuntimeException notFocused) {
            focused = false; // the camera stopped, or a tap started a new focus
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            focused = false;
        }
        boolean stillHere = !isDestroyed() && cameraBound && photo == currentPhoto()
                && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        if (!stillHere) {
            return; // the screen closed, or moved on, while the camera was focusing
        }
        if (focused) {
            capture(photo);
        } else {
            notFocusedFor = photo;
            showCameraStatus();
        }
    }

    private void capture(@NonNull Photo photo) {
        File file = viewModel.startCapture(photo);
        if (file == null) {
            return;
        }
        // The photo goes straight into ReadyShelf's private folder: no other app gets it.
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file).build();
        try {
            camera.takePicture(options, mainThread, viewModel.captureCallback(photo, file));
        } catch (RuntimeException notReady) {
            viewModel.captureFailed(photo, file);
        }
    }

    private void choosePhoto() {
        try {
            pickPhoto.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } catch (ActivityNotFoundException noPicker) {
            Toast.makeText(this, R.string.photo_no_picker, Toast.LENGTH_LONG).show();
        }
    }

    private void onPhotoPicked(@Nullable Uri picked) {
        if (picked != null) { // null means the household closed the picker
            viewModel.photoPicked(picked);
        }
    }

    private void skipPhoto() {
        Photo photo = currentPhoto();
        if (photo != null) {
            viewModel.skip(photo);
        }
    }

    /** ReadyShelf's app page in Android's settings, where the camera can be allowed. */
    private void openAppSettings() {
        Intent appPage = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        try {
            startActivity(appPage);
        } catch (ActivityNotFoundException noSuchPage) {
            // A dialog, not a toast: Android shows only two lines of a toast.
            openDialog = new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.photo_settings_missing)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    // ----- Showing the steps and the review -----

    private void show(@NonNull State state) {
        boolean review = state.step == Step.REVIEW;
        captureGroup.setVisibility(review ? View.GONE : View.VISIBLE);
        reviewScroll.setVisibility(review ? View.VISIBLE : View.GONE);
        if (review) {
            showReview(state);
        } else {
            showStep(state);
        }
        updateCamera();
    }

    private void showStep(@NonNull State state) {
        boolean product = state.step == Step.PRODUCT;
        stepTitle.setText(product ? R.string.photo_step_product : R.string.photo_step_date);
        stepHint.setText(product ? R.string.photo_step_product_hint : R.string.photo_step_date_hint);
        preview.setContentDescription(getString(product ? R.string.photo_preview_product
                : R.string.photo_preview_date));
        boolean saving = (product ? state.productState : state.dateState) == PhotoState.SAVING;
        takeButton.setEnabled(!saving);
        chooseButton.setEnabled(!saving);
        skipButton.setEnabled(!saving);
    }

    private void showReview(@NonNull State state) {
        String status = state.busy ? getString(R.string.photo_reading) : noticeText(state.notice);
        reviewStatus.setText(status);
        reviewStatus.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
        reviewProgress.setVisibility(state.busy ? View.VISIBLE : View.GONE);

        productText.setText(photoText(state.productState, state.productLines));
        productNote.setVisibility(state.productState == PhotoState.SKIPPED ? View.GONE : View.VISIBLE);
        dateText.setText(photoText(state.dateState, state.dateLines));

        datesMessage.setText(datesMessageText(state));
        PhotoDateChoices choices = state.dateState == PhotoState.READ ? state.dateChoices : null;
        if (choices != shownChoices) {
            shownChoices = choices;
            showDateChoices(choices);
        }
        // Nothing goes back to the form while a photo is still being read.
        for (int i = 0; i < dateChoices.getChildCount(); i++) {
            View child = dateChoices.getChildAt(i);
            if (child instanceof Button) {
                child.setEnabled(!state.busy);
            }
        }
        continueButton.setEnabled(!state.busy);
    }

    /** The text read on one photo, or what happened to it. */
    @NonNull
    private String photoText(@NonNull PhotoState photoState, @NonNull List<String> lines) {
        switch (photoState) {
            case SKIPPED:
                return getString(R.string.photo_text_skipped);
            case NOT_READ:
                return getString(R.string.photo_text_failed);
            case READ:
                return lines.isEmpty() ? getString(R.string.photo_text_none) : String.join("\n", lines);
            default:
                return getString(R.string.photo_text_reading);
        }
    }

    @NonNull
    private String datesMessageText(@NonNull State state) {
        switch (state.dateState) {
            case SKIPPED:
                return getString(R.string.photo_dates_no_photo);
            case NOT_READ:
                return getString(R.string.photo_dates_not_read);
            case READ:
                break;
            default:
                return getString(R.string.photo_dates_waiting);
        }
        PhotoDateChoices choices = state.dateChoices;
        if (choices == null) {
            return getString(R.string.photo_dates_waiting);
        }
        switch (choices.getOutcome()) {
            case NOT_CONNECTED:
                return getString(R.string.photo_dates_not_connected);
            case NO_TEXT:
                return getString(R.string.photo_dates_no_text);
            case NO_DATE_FOUND:
                return getString(R.string.photo_dates_none);
            case PARSER_FAILED:
                // The error's details are not shown or logged: they could contain label text.
                return getString(R.string.photo_dates_failed);
            default:
                return getString(R.string.photo_dates_choose);
        }
    }

    /** One button per possible date. Two readings of one date come after a note. */
    private void showDateChoices(@Nullable PhotoDateChoices choices) {
        dateChoices.removeAllViews();
        if (choices == null || choices.getOutcome() != PhotoDateChoices.Outcome.CHOICES) {
            return;
        }
        for (PhotoDateChoices.Choice choice : choices.getChoices()) {
            if (choice.getReading() == PhotoDateChoices.Choice.Reading.MONTH_FIRST) {
                // The theme's body text (?attr/textAppearanceBodyLarge, as in ReadyShelf.Body),
                // the same size and line height as the other texts on this screen. Until
                // 2026-10-19 the note used setTextAppearance(R.style.ReadyShelf_Body), which isn't
                // a text appearance, so it got Android's smaller default text. A MaterialTextView
                // also takes the line height from the text appearance; a plain TextView doesn't.
                TextView note = new MaterialTextView(this);
                TypedValue bodyText = new TypedValue();
                getTheme().resolveAttribute(
                        com.google.android.material.R.attr.textAppearanceBodyLarge, bodyText, true);
                note.setTextAppearance(bodyText.resourceId);
                note.setText(getString(R.string.photo_two_readings_note, choice.getDateText()));
                note.setPadding(0, getResources().getDimensionPixelSize(R.dimen.space_medium), 0, 0);
                dateChoices.addView(note);
            }
            dateChoices.addView(choiceButton(choiceText(choice), view -> finishWith(choice)));
        }
    }

    @NonNull
    private String choiceText(@NonNull PhotoDateChoices.Choice choice) {
        switch (choice.getReading()) {
            case MONTH_FIRST:
                return getString(R.string.photo_use_month_first, PhotoText.date(this, choice),
                        choice.getDateText());
            case DAY_FIRST:
                return getString(R.string.photo_use_day_first, PhotoText.date(this, choice),
                        choice.getDateText());
            case AS_PRINTED:
                return getString(R.string.photo_use_as_printed, choice.getDateText());
            case MONTH_AND_YEAR:
                return getString(R.string.photo_use_month_and_year, choice.getDateText());
            default:
                return getString(R.string.photo_use_date, PhotoText.date(this, choice),
                        choice.getRawText());
        }
    }

    /** A full-width outlined button whose text wraps onto more lines when it is long. */
    @NonNull
    private MaterialButton choiceButton(@NonNull String text, @NonNull View.OnClickListener onClick) {
        MaterialButton button = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        button.setMaxLines(8);
        button.setOnClickListener(onClick);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        size.topMargin = dp(8);
        button.setLayoutParams(size);
        return button;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    /** Goes back to the form with the date the household tapped, or with no date. */
    private void finishWith(@Nullable PhotoDateChoices.Choice chosenDate) {
        State state = viewModel.getState().getValue();
        if (state == null || state.busy) {
            return;
        }
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_RESULT, viewModel.result(chosenDate)));
        finish();
    }
}

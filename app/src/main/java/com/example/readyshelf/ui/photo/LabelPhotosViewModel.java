package com.example.readyshelf.ui.photo;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.LabelPhotoFiles;
import com.example.readyshelf.data.LabelPhotoReader;
import com.example.readyshelf.domain.DateCandidateParserContract;
import com.example.readyshelf.domain.PhotoDateChoices;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Keeps the steps of the Label photos screen, reads each photo's text on the phone, and asks the
 * student's date-candidate parser for possible dates (SPEC.md, "Photo entry").
 *
 * <p>Reading the text and finding dates are separate: LabelPhotoReader turns a photo into text
 * lines and knows nothing about dates, and the parser turns the date photo's lines into possible
 * dates and never sees a photo. The parser runs on the reader's background thread, through
 * PhotoDateChoices, so a mistake in it can't close the app.
 *
 * <p>What the screen shows is kept in a SavedStateHandle, so it survives rotation and Android
 * closing the app. Photos are temporary: the reader deletes each one after reading it, and the
 * photo folder is emptied when this ViewModel is made and when the screen closes for good.
 */
public class LabelPhotosViewModel extends AndroidViewModel {

    /** The two photos. */
    public enum Photo {
        PRODUCT,
        DATE
    }

    /** Where one photo is. */
    public enum PhotoState {
        /** Not taken yet: its step is shown. */
        WAITING,
        /** The camera is saving it. */
        SAVING,
        /** Its text is being read. */
        READING,
        /** Its text was read (possibly no text at all). */
        READ,
        /** It could not be read. */
        NOT_READ,
        /** The household skipped it. */
        SKIPPED
    }

    /** What the screen shows. */
    public enum Step {
        PRODUCT,
        DATE,
        REVIEW
    }

    /** A message about the last photo, shown until the household's next step. */
    public enum Notice {
        NONE,
        /** The camera could not save the photo. */
        NOT_SAVED,
        /** Android closed the app while the photo was being saved or read. */
        LOST
    }

    /** Everything the screen shows. */
    public static final class State {
        @NonNull public Step step = Step.PRODUCT;
        @NonNull public PhotoState productState = PhotoState.WAITING;
        @NonNull public PhotoState dateState = PhotoState.WAITING;
        @NonNull public List<String> productLines = Collections.emptyList();
        @NonNull public List<String> dateLines = Collections.emptyList();
        // The possible dates for the date photo, once it has been read.
        @Nullable public PhotoDateChoices dateChoices;
        @NonNull public Notice notice = Notice.NONE;
        // A photo is being saved or read, so the result buttons wait.
        public boolean busy;
    }

    private static final String KEY_PRODUCT_STATE = "productState";
    private static final String KEY_DATE_STATE = "dateState";
    private static final String KEY_PRODUCT_LINES = "productLines";
    private static final String KEY_DATE_LINES = "dateLines";
    private static final String KEY_DATE_CHOICES = "dateChoices";
    private static final String KEY_NOTICE = "notice";
    private static final String KEY_PERMISSION_ASKED = "permissionAsked";

    private final AppContainer app;
    private final SavedStateHandle saved;
    private final LabelPhotoFiles files;
    private final Executor mainThread;
    private final MutableLiveData<State> state = new MutableLiveData<>();

    // Each new photo request gets a new number. A result for an older number (for example
    // after "Take it again", or after the screen closed) is dropped and its file deleted.
    private int productRequest;
    private int dateRequest;
    private boolean closed;

    public LabelPhotosViewModel(@NonNull Application application, @NonNull SavedStateHandle savedState) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        saved = savedState;
        files = new LabelPhotoFiles(application.getCacheDir());
        mainThread = ContextCompat.getMainExecutor(application);

        // A file still in the folder is left over from a session that couldn't clean up, for
        // example when Android closed the app while a photo was being read. No photo is kept.
        files.deleteAll();
        // A photo that was being saved or read when Android closed the app is gone.
        for (Photo photo : Photo.values()) {
            PhotoState was = photoState(photo);
            if (was == PhotoState.SAVING || was == PhotoState.READING) {
                setPhotoState(photo, PhotoState.WAITING);
                saved.set(KEY_NOTICE, Notice.LOST);
            }
        }
        publish();
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    // ----- Taking and choosing photos -----

    /**
     * Gets a new private file for the photo that the screen is about to take, and marks the photo
     * as being saved. Returns null when the folder can't be made (the notice says so).
     */
    @Nullable
    @MainThread
    public File startCapture(@NonNull Photo photo) {
        File file;
        try {
            file = files.newPhotoFile(photo == Photo.PRODUCT ? "product" : "date");
        } catch (IOException noFolder) {
            saved.set(KEY_NOTICE, Notice.NOT_SAVED);
            publish();
            return null;
        }
        nextRequest(photo);
        setPhotoState(photo, PhotoState.SAVING);
        saved.set(KEY_NOTICE, Notice.NONE);
        publish();
        return file;
    }

    /**
     * The callback for the photo that {@link #startCapture} prepared. It holds only this
     * ViewModel, never the screen, so a photo that finishes after the screen rotated still
     * arrives. CameraX calls exactly one of its two methods.
     */
    @NonNull
    public ImageCapture.OnImageSavedCallback captureCallback(@NonNull Photo photo, @NonNull File file) {
        return new CaptureCallback(this, photo, file, request(photo));
    }

    /** The camera could not even start taking the photo. */
    @MainThread
    public void captureFailed(@NonNull Photo photo, @NonNull File file) {
        photoNotSaved(photo, file, request(photo));
    }

    /**
     * The household picked a saved photo for the step on screen. It is only read: ReadyShelf
     * doesn't copy, change, or delete it.
     */
    @MainThread
    public void photoPicked(@NonNull Uri picked) {
        Step step = step();
        if (step == Step.REVIEW) {
            return;
        }
        Photo photo = step == Step.PRODUCT ? Photo.PRODUCT : Photo.DATE;
        saved.set(KEY_NOTICE, Notice.NONE);
        readPhoto(photo, picked, null, nextRequest(photo));
    }

    /** The household skipped the photo on screen. */
    @MainThread
    public void skip(@NonNull Photo photo) {
        nextRequest(photo);
        clearText(photo);
        setPhotoState(photo, PhotoState.SKIPPED);
        saved.set(KEY_NOTICE, Notice.NONE);
        publish();
    }

    /** "Take it again": the photo's step is shown again, and its old text is forgotten. */
    @MainThread
    public void retake(@NonNull Photo photo) {
        nextRequest(photo);
        clearText(photo);
        setPhotoState(photo, PhotoState.WAITING);
        saved.set(KEY_NOTICE, Notice.NONE);
        publish();
    }

    @MainThread
    void photoSaved(@NonNull Photo photo, @NonNull File file, int request) {
        if (closed || request != request(photo)) {
            delete(file); // the screen closed, or the household asked for another photo
            return;
        }
        // The reader deletes the file when it has read it.
        readPhoto(photo, Uri.fromFile(file), file, request);
    }

    @MainThread
    void photoNotSaved(@NonNull Photo photo, @NonNull File file, int request) {
        delete(file);
        if (closed || request != request(photo)) {
            return;
        }
        setPhotoState(photo, PhotoState.WAITING);
        saved.set(KEY_NOTICE, Notice.NOT_SAVED);
        publish();
    }

    private void readPhoto(@NonNull Photo photo, @NonNull Uri uri, @Nullable File deleteAfterReading,
                           int request) {
        setPhotoState(photo, PhotoState.READING);
        publish();
        DateCandidateParserContract parser = app.dateCandidateParser;
        app.labelPhotoReader.read(uri, deleteAfterReading, new LabelPhotoReader.Listener() {
            @Override
            public void onRead(@NonNull List<String> lines) {
                // This is the reader's background thread, so the parser never runs on the main
                // thread. PhotoDateChoices catches the parser's errors itself.
                PhotoDateChoices choices = photo == Photo.DATE
                        ? PhotoDateChoices.find(parser, lines, LocalDate.now())
                        : null;
                mainThread.execute(() -> photoRead(photo, request, lines, choices));
            }

            @Override
            public void onFailed(@NonNull Throwable error) {
                mainThread.execute(() -> photoNotRead(photo, request));
            }
        });
    }

    @MainThread
    private void photoRead(@NonNull Photo photo, int request, @NonNull List<String> lines,
                           @Nullable PhotoDateChoices choices) {
        if (closed || request != request(photo)) {
            return;
        }
        saved.set(linesKey(photo), new ArrayList<>(lines));
        if (photo == Photo.DATE) {
            saved.set(KEY_DATE_CHOICES, choices);
        }
        setPhotoState(photo, PhotoState.READ);
        publish();
    }

    @MainThread
    private void photoNotRead(@NonNull Photo photo, int request) {
        if (closed || request != request(photo)) {
            return;
        }
        clearText(photo);
        setPhotoState(photo, PhotoState.NOT_READ);
        publish();
    }

    // ----- The result and the camera permission -----

    /**
     * What goes back to the form: the date the household tapped (or null for "Continue without
     * a date"), and suggestions from the text.
     */
    @NonNull
    public PhotoEntryResult result(@Nullable PhotoDateChoices.Choice chosenDate) {
        return PhotoEntryResult.of(chosenDate, lines(Photo.PRODUCT), lines(Photo.DATE));
    }

    /** True until the screen has asked for the camera permission once by itself. */
    public boolean shouldAskForCamera() {
        Boolean asked = saved.get(KEY_PERMISSION_ASKED);
        return asked == null || !asked;
    }

    /** The screen asked for the camera permission; it doesn't ask again by itself. */
    public void askedForCamera() {
        saved.set(KEY_PERMISSION_ASKED, true);
    }

    @Override
    protected void onCleared() {
        // The screen closed for good (a date was chosen, Cancel, Back, or the back arrow).
        // Rotation doesn't come here. No photo is kept.
        closed = true;
        files.deleteAll();
    }

    // ----- Helpers -----

    /**
     * The first photo that is not taken yet, or still being saved: the screen stays on a photo's
     * step until the camera has saved it, so the household holds still until then.
     */
    @NonNull
    private Step step() {
        if (onItsStep(photoState(Photo.PRODUCT))) {
            return Step.PRODUCT;
        }
        if (onItsStep(photoState(Photo.DATE))) {
            return Step.DATE;
        }
        return Step.REVIEW;
    }

    private static boolean onItsStep(@NonNull PhotoState photoState) {
        return photoState == PhotoState.WAITING || photoState == PhotoState.SAVING;
    }

    @NonNull
    private PhotoState photoState(@NonNull Photo photo) {
        PhotoState value = saved.get(photo == Photo.PRODUCT ? KEY_PRODUCT_STATE : KEY_DATE_STATE);
        return value != null ? value : PhotoState.WAITING;
    }

    private void setPhotoState(@NonNull Photo photo, @NonNull PhotoState value) {
        saved.set(photo == Photo.PRODUCT ? KEY_PRODUCT_STATE : KEY_DATE_STATE, value);
    }

    @NonNull
    private static String linesKey(@NonNull Photo photo) {
        return photo == Photo.PRODUCT ? KEY_PRODUCT_LINES : KEY_DATE_LINES;
    }

    @NonNull
    private List<String> lines(@NonNull Photo photo) {
        ArrayList<String> value = saved.get(linesKey(photo));
        return value != null ? Collections.unmodifiableList(value) : Collections.emptyList();
    }

    private void clearText(@NonNull Photo photo) {
        saved.set(linesKey(photo), null);
        if (photo == Photo.DATE) {
            saved.set(KEY_DATE_CHOICES, null);
        }
    }

    private int request(@NonNull Photo photo) {
        return photo == Photo.PRODUCT ? productRequest : dateRequest;
    }

    private int nextRequest(@NonNull Photo photo) {
        return photo == Photo.PRODUCT ? ++productRequest : ++dateRequest;
    }

    private static void delete(@NonNull File file) {
        if (file.exists()) {
            file.delete(); // if this fails, the folder is emptied again when the screen closes
        }
    }

    private void publish() {
        State next = new State();
        next.step = step();
        next.productState = photoState(Photo.PRODUCT);
        next.dateState = photoState(Photo.DATE);
        next.productLines = lines(Photo.PRODUCT);
        next.dateLines = lines(Photo.DATE);
        next.dateChoices = saved.get(KEY_DATE_CHOICES);
        Notice notice = saved.get(KEY_NOTICE);
        next.notice = notice != null ? notice : Notice.NONE;
        next.busy = isBusy(next.productState) || isBusy(next.dateState);
        state.setValue(next);
    }

    private static boolean isBusy(@NonNull PhotoState photoState) {
        return photoState == PhotoState.SAVING || photoState == PhotoState.READING;
    }

    /** Hands CameraX's answer to the ViewModel. It never holds the screen. */
    private static final class CaptureCallback implements ImageCapture.OnImageSavedCallback {
        private final LabelPhotosViewModel viewModel;
        private final Photo photo;
        private final File file;
        private final int request;

        CaptureCallback(@NonNull LabelPhotosViewModel viewModel, @NonNull Photo photo,
                        @NonNull File file, int request) {
            this.viewModel = viewModel;
            this.photo = photo;
            this.file = file;
            this.request = request;
        }

        @Override
        public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
            viewModel.photoSaved(photo, file, request);
        }

        @Override
        public void onError(@NonNull ImageCaptureException error) {
            viewModel.photoNotSaved(photo, file, request);
        }
    }
}

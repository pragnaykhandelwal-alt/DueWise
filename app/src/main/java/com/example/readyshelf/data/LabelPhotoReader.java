package com.example.readyshelf.data;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads the text on a label photo with Tesseract, on the phone. Nothing is sent anywhere.
 *
 * <p>Reading takes a few seconds, so it runs on this class's own background thread. A Tesseract
 * object must stay on one thread, so each reading makes its own TessBaseAPI on that thread and
 * frees it at the end.
 */
public final class LabelPhotoReader {

    /** Gets the result of one reading. It is called on the reader's background thread. */
    public interface Listener {
        void onRead(@NonNull List<String> lines);

        void onFailed(@NonNull Throwable error);
    }

    private static final String TAG = "LabelPhotoReader";

    // English language data from tessdata_fast (see TOOLCHAIN.md). If the file in
    // app/src/main/assets changes, update MODEL_BYTES so the new file is copied.
    private static final String MODEL_ASSET = "tessdata/eng.traineddata";
    private static final long MODEL_BYTES = 4_113_088L;
    private static final String LANGUAGE = "eng";

    // About 2400 pixels on the longest side is enough for label text and keeps memory use low.
    private static final int MAX_SIDE_PX = 2400;

    // Page mode 11 ("sparse text") looks for text scattered over the picture, as on a package.
    private static final int PAGE_MODE = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT;

    private final Context appContext;
    private final ExecutorService ocrThread = Executors.newSingleThreadExecutor();

    public LabelPhotoReader(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * Reads a photo on the background thread and tells the listener (on that thread).
     *
     * @param deleteAfterReading a temporary photo file to delete when reading ends, or null
     */
    public void read(@NonNull Uri photo, @Nullable File deleteAfterReading, @NonNull Listener listener) {
        ocrThread.execute(() -> {
            try {
                listener.onRead(readLines(photo));
            } catch (Exception | OutOfMemoryError error) {
                Log.e(TAG, "Could not read the label photo", error);
                listener.onFailed(error);
            } finally {
                if (deleteAfterReading != null && deleteAfterReading.exists()
                        && !deleteAfterReading.delete()) {
                    Log.w(TAG, "Could not delete the temporary label photo");
                }
            }
        });
    }

    /** The photo's text lines, top to bottom. This blocks, so call it on a background thread. */
    @NonNull
    private List<String> readLines(@NonNull Uri photo) throws IOException {
        long startedAt = SystemClock.elapsedRealtime();
        File dataFolder = ensureLanguageData();
        Bitmap picture = decodeUpright(photo);
        TessBaseAPI tesseract = new TessBaseAPI();
        try {
            boolean ready;
            try {
                ready = tesseract.init(dataFolder.getAbsolutePath(), LANGUAGE);
            } catch (IllegalArgumentException missingData) {
                throw new IOException("Tesseract language data not found", missingData);
            }
            if (!ready) {
                throw new IOException("Tesseract could not start");
            }
            tesseract.setVariable("user_defined_dpi", "300");
            tesseract.setPageSegMode(PAGE_MODE);
            tesseract.setImage(picture);
            picture.recycle(); // Tesseract keeps its own copy, so free this one now
            List<String> lines = splitLines(tesseract.getUTF8Text());
            // Only the time and the line count are logged, never the text.
            Log.i(TAG, "Read " + lines.size() + " lines in "
                    + (SystemClock.elapsedRealtime() - startedAt) + " ms");
            return lines;
        } finally {
            if (!picture.isRecycled()) {
                picture.recycle();
            }
            tesseract.recycle();
        }
    }

    /**
     * Tesseract reads its language data from a "tessdata" folder in the app's private files.
     * The first reading copies it there from the app's assets. It writes a ".part" file first and
     * then renames it, so an interrupted copy never leaves a broken file. Returns the folder that
     * contains "tessdata".
     */
    @NonNull
    private File ensureLanguageData() throws IOException {
        File dataFolder = new File(appContext.getFilesDir(), "ocr");
        File tessdata = new File(dataFolder, "tessdata");
        File model = new File(tessdata, LANGUAGE + ".traineddata");
        if (model.isFile() && model.length() == MODEL_BYTES) {
            return dataFolder;
        }
        if (!tessdata.isDirectory() && !tessdata.mkdirs()) {
            throw new IOException("Could not create the language data folder");
        }
        File part = new File(tessdata, LANGUAGE + ".traineddata.part");
        try (InputStream in = appContext.getAssets().open(MODEL_ASSET);
             OutputStream out = new FileOutputStream(part)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }
        if (!part.renameTo(model)) {
            part.delete();
            throw new IOException("Could not save the language data");
        }
        return dataFolder;
    }

    /** Decodes a smaller copy of the photo, turned upright and drawn on white. */
    @NonNull
    private Bitmap decodeUpright(@NonNull Uri photo) throws IOException {
        ContentResolver resolver = appContext.getContentResolver();

        // Step 1: read only the picture's size.
        BitmapFactory.Options sizeOnly = new BitmapFactory.Options();
        sizeOnly.inJustDecodeBounds = true;
        try (InputStream in = open(resolver, photo)) {
            BitmapFactory.decodeStream(in, null, sizeOnly);
        }
        if (sizeOnly.outWidth <= 0 || sizeOnly.outHeight <= 0) {
            throw new IOException("Not a picture");
        }

        // Step 2: decode it, halving the size until the longest side is at most MAX_SIDE_PX.
        int sampleSize = 1;
        while (Math.max(sizeOnly.outWidth, sizeOnly.outHeight) / sampleSize > MAX_SIDE_PX) {
            sampleSize *= 2;
        }
        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = sampleSize;
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream in = open(resolver, photo)) {
            decoded = BitmapFactory.decodeStream(in, null, decode);
        }
        if (decoded == null) {
            throw new IOException("Could not decode the picture");
        }

        // Step 3: turn it upright, as the camera recorded, on a white background
        // (transparent pixels would otherwise be read as black).
        return uprightOnWhite(decoded, rotationDegrees(resolver, photo));
    }

    @NonNull
    private static InputStream open(@NonNull ContentResolver resolver, @NonNull Uri uri) throws IOException {
        InputStream in = resolver.openInputStream(uri);
        if (in == null) {
            throw new IOException("Could not open the picture");
        }
        return in;
    }

    /**
     * How far the camera says to turn the picture (EXIF orientation). 0 when unknown.
     *
     * <p>This uses Android's built-in ExifInterface, so no extra library is needed. Lint suggests
     * the AndroidX version because of bugs in older Android versions. ReadyShelf needs Android 8
     * (API 26) or newer; switching to the AndroidX library is a possible follow-up (TOOLCHAIN.md).
     */
    @SuppressLint("ExifInterface")
    private static int rotationDegrees(@NonNull ContentResolver resolver, @NonNull Uri photo) {
        try (InputStream in = open(resolver, photo)) {
            android.media.ExifInterface exif = new android.media.ExifInterface(in);
            int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL);
            if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            } else if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            } else if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
            return 0;
        } catch (IOException | RuntimeException noExif) {
            return 0; // pictures without EXIF (for example PNG) are already upright
        }
    }

    /** Draws the picture turned by {@code degrees} onto a new white ARGB_8888 picture. */
    @NonNull
    private static Bitmap uprightOnWhite(@NonNull Bitmap source, int degrees) {
        boolean sideways = degrees == 90 || degrees == 270;
        int width = sideways ? source.getHeight() : source.getWidth();
        int height = sideways ? source.getWidth() : source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);
        Matrix turn = new Matrix();
        turn.postRotate(degrees);
        // Turning moves the picture out of view, so move it back.
        if (degrees == 90) {
            turn.postTranslate(source.getHeight(), 0);
        } else if (degrees == 180) {
            turn.postTranslate(source.getWidth(), source.getHeight());
        } else if (degrees == 270) {
            turn.postTranslate(0, source.getWidth());
        }
        canvas.drawBitmap(source, turn, new Paint(Paint.FILTER_BITMAP_FLAG));
        source.recycle();
        return result;
    }

    @NonNull
    private static List<String> splitLines(@Nullable String text) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}

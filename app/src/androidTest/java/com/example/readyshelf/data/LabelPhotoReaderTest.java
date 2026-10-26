package com.example.readyshelf.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Reads synthetic label pictures (drawn text in androidTest/assets/labels, not photos of real
 * packages) with the app's real text reader (Tesseract), on the device. Run it on the emulator
 * only, with airplane mode on, to show that reading works offline (TOOLCHAIN.md).
 *
 * <p>Only text extraction is tested here. Finding dates in the text is the student's
 * date-candidate parser, which this test doesn't use.
 */
@RunWith(AndroidJUnit4.class)
public class LabelPhotoReaderTest {

    private static final String TAG = "LabelPhotoReaderTest";
    private static final long WAIT_SECONDS = 90;

    private LabelPhotoReader reader;
    private LabelPhotoFiles files;

    /** What one reading gave: the lines, or the error. */
    private static final class Reading {
        volatile List<String> lines;
        volatile Throwable error;
        File photo;

        /** All lines in capitals, without spaces, so "10/25/ 2026" still counts. */
        String compact() {
            return String.join("", lines).replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        }
    }

    @Before
    public void setUp() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        reader = new LabelPhotoReader(app);
        files = new LabelPhotoFiles(app.getCacheDir());
    }

    @After
    public void tearDown() {
        files.deleteAll();
    }

    @Test
    public void aClearLabel_givesItsText() throws Exception {
        Reading reading = read("label_clear.png");

        assertNull(reading.error);
        assertTrue(reading.compact(), reading.compact().contains("10/25/2026"));
        assertTrue(reading.compact(), reading.compact().contains("GRANOLA"));
    }

    @Test
    public void severalDates_areAllInTheText() throws Exception {
        Reading reading = read("label_several_dates.png");

        assertTrue(reading.compact(), reading.compact().contains("11/20/2026"));
        assertTrue(reading.compact(), reading.compact().contains("10/25/2026"));
    }

    @Test
    public void aDateWithTwoReadings_isReadAsPrinted() throws Exception {
        Reading reading = read("label_two_readings.png");

        assertTrue(reading.compact(), reading.compact().contains("03/04/2027"));
    }

    @Test
    public void aLabelWithNoDate_givesTextWithoutADate() throws Exception {
        Reading reading = read("label_no_date.png");

        assertTrue(reading.compact(), reading.compact().contains("NUTRITION"));
        assertFalse(reading.compact(), reading.compact().contains("/"));
    }

    @Test
    public void aBlurryLabel_stillFinishes() throws Exception {
        // What a blurry photo gives is unclear text or none; the review shows it as it is.
        read("label_blurry.png");
    }

    @Test
    public void aDarkLabel_stillFinishes() throws Exception {
        read("label_dark.png");
    }

    /** Copies one test picture into the photo folder, reads it, and checks that it was deleted. */
    @NonNull
    private Reading read(@NonNull String asset) throws IOException, InterruptedException {
        Reading reading = new Reading();
        reading.photo = files.newPhotoFile("test");
        Context testApk = InstrumentationRegistry.getInstrumentation().getContext();
        try (InputStream in = testApk.getAssets().open("labels/" + asset);
             OutputStream out = new FileOutputStream(reading.photo)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }

        CountDownLatch done = new CountDownLatch(1);
        reader.read(Uri.fromFile(reading.photo), reading.photo, new LabelPhotoReader.Listener() {
            @Override
            public void onRead(@NonNull List<String> lines) {
                reading.lines = lines;
                done.countDown();
            }

            @Override
            public void onFailed(@NonNull Throwable error) {
                reading.error = error;
                done.countDown();
            }
        });
        assertTrue("The reading of " + asset + " did not finish", done.await(WAIT_SECONDS, TimeUnit.SECONDS));
        // These are synthetic pictures, so their text may be logged for the test record.
        Log.i(TAG, asset + ": " + (reading.lines != null ? reading.lines.size() + " lines "
                + reading.lines : "error " + reading.error));
        if (reading.error == null) {
            assertNotNull(reading.lines);
        }

        // The reader deletes the photo right after telling the listener.
        long until = SystemClock.elapsedRealtime() + 5000;
        while (reading.photo.exists() && SystemClock.elapsedRealtime() < until) {
            SystemClock.sleep(50);
        }
        assertFalse("The temporary photo was not deleted", reading.photo.exists());
        return reading;
    }
}

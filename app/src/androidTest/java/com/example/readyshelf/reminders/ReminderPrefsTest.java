package com.example.readyshelf.reminders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * The daily reminder record on the phone: it is on disk as soon as recordShown returns, so an
 * app restart (a new process reads the file) finds it. Android keeps preferences in memory
 * while the app runs, so this test reads the file itself. Uses its own file.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReminderPrefsTest {

    private static final String FILE = "reminders-prefs-test";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    @After
    public void deleteTestFile() {
        context.deleteSharedPreferences(FILE);
    }

    @Test
    public void theRecord_isOnDiskAsSoonAsRecordShownReturns() throws Exception {
        ReminderPrefs prefs = new ReminderPrefs(context, FILE);

        assertTrue(prefs.recordShown(DAY));

        File file = new File(context.getApplicationInfo().dataDir, "shared_prefs/" + FILE + ".xml");
        assertTrue(file.getPath(), file.isFile());
        String saved = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(saved, saved.contains("2026-10-11"));
    }

    @Test
    public void aNewObject_readsTheRecordAndTheSwitch() {
        ReminderPrefs prefs = new ReminderPrefs(context, FILE);
        assertFalse("reminders start off", prefs.isOn());
        assertNull(prefs.lastShownDay());

        prefs.setOn(true);
        assertTrue(prefs.recordShown(DAY));

        ReminderPrefs again = new ReminderPrefs(context, FILE);
        assertTrue(again.isOn());
        assertEquals(DAY, again.lastShownDay());
    }
}

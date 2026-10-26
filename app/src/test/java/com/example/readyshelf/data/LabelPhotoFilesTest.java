package com.example.readyshelf.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Tests for the private folder of temporary label photos. A test folder stands in for the cache. */
public class LabelPhotoFilesTest {

    @Rule
    public TemporaryFolder cache = new TemporaryFolder();

    @Test
    public void newPhotoFile_isANewNameInsideThePhotoFolder() throws IOException {
        LabelPhotoFiles files = new LabelPhotoFiles(cache.getRoot());

        File photo = files.newPhotoFile("date");

        assertEquals(new File(cache.getRoot(), "label_photos"), photo.getParentFile());
        assertTrue(photo.getParentFile().isDirectory());
        assertTrue(photo.getName().startsWith("date-"));
        assertTrue(photo.getName().endsWith(".jpg"));
        assertFalse(photo.exists()); // the camera writes it
    }

    @Test
    public void everyPhotoGetsItsOwnName() throws IOException {
        LabelPhotoFiles files = new LabelPhotoFiles(cache.getRoot());

        assertNotEquals(files.newPhotoFile("product"), files.newPhotoFile("product"));
    }

    @Test
    public void deleteAll_deletesEveryFile_alsoOnesTheCameraLeftBehind() throws IOException {
        LabelPhotoFiles files = new LabelPhotoFiles(cache.getRoot());
        File photo = files.newPhotoFile("product");
        write(photo);
        File leftOver = new File(files.getFolder(), "camera-temp.tmp");
        write(leftOver);
        File oldPhoto = new File(files.getFolder(), "label.jpg"); // from the scan before 2026-10-12
        write(oldPhoto);

        assertEquals(0, files.deleteAll());

        assertFalse(photo.exists());
        assertFalse(leftOver.exists());
        assertFalse(oldPhoto.exists());
    }

    @Test
    public void deleteAll_withNoFolder_doesNothing() {
        assertEquals(0, new LabelPhotoFiles(cache.getRoot()).deleteAll());
    }

    @Test
    public void deleteAll_leavesOtherCacheFilesAlone() throws IOException {
        File other = cache.newFile("other.txt");
        LabelPhotoFiles files = new LabelPhotoFiles(cache.getRoot());
        write(files.newPhotoFile("date"));

        files.deleteAll();

        assertTrue(other.exists());
    }

    @Test(expected = IOException.class)
    public void newPhotoFile_failsWhenTheFolderCantBeMade() throws IOException {
        // A file with the folder's name stops the folder from being made.
        write(new File(cache.getRoot(), "label_photos"));

        new LabelPhotoFiles(cache.getRoot()).newPhotoFile("date");
    }

    private static void write(File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(new byte[] {1, 2, 3});
        }
    }
}

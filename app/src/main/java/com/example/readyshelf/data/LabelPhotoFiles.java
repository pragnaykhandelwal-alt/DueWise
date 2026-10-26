package com.example.readyshelf.data;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * The folder for temporary label photos: cache/label_photos/ in ReadyShelf's own private storage.
 *
 * <p>Other apps can't read it, and backup and phone-to-phone transfer exclude it. A photo lives
 * there only until it has been read: the text reader deletes each photo after reading it, and
 * photo entry empties the folder whenever a photo session starts or ends (SPEC.md, "Photo
 * entry").
 */
public final class LabelPhotoFiles {

    /** The folder's name inside the app's cache folder. */
    static final String FOLDER = "label_photos";

    @NonNull private final File folder;

    /** @param cacheDir the app's private cache folder ({@code Context.getCacheDir()}) */
    public LabelPhotoFiles(@NonNull File cacheDir) {
        folder = new File(cacheDir, FOLDER);
    }

    /**
     * A new file name for one photo, for example "date-…jpg". The file itself is not made yet;
     * the camera writes it. Every name is different, so an old photo is never read as a new one.
     *
     * @param prefix "product" or "date"
     * @throws IOException when the folder can't be made
     */
    @NonNull
    public File newPhotoFile(@NonNull String prefix) throws IOException {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Could not make the label photo folder");
        }
        return new File(folder, prefix + "-" + UUID.randomUUID() + ".jpg");
    }

    /**
     * Deletes every file in the folder, also any file that the camera library left behind.
     * Returns how many files could not be deleted (0 when the folder is empty or missing).
     */
    public int deleteAll() {
        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }
        int notDeleted = 0;
        for (File file : files) {
            if (file.exists() && !file.delete()) {
                notDeleted++;
            }
        }
        return notDeleted;
    }

    @NonNull
    public File getFolder() {
        return folder;
    }
}

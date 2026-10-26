package com.example.readyshelf.data;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

import java.time.LocalDate;

/**
 * Tells Room how to store a type that SQLite doesn't know.
 *
 * <p>A LocalDate is stored as text such as "2026-10-12". Text in this form sorts
 * in date order. A missing date (null) stays null in both directions.
 */
public final class Converters {

    private Converters() {
        // Room calls the static methods; no objects of this class are made.
    }

    @TypeConverter
    @Nullable
    public static String fromLocalDate(@Nullable LocalDate date) {
        return date == null ? null : date.toString();
    }

    @TypeConverter
    @Nullable
    public static LocalDate toLocalDate(@Nullable String text) {
        return text == null ? null : LocalDate.parse(text);
    }
}

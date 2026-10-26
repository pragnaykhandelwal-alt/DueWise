package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.Objects;

/** A category that CategoryGuesser read from a label, and the words that led to it. Never changes. */
public final class CategoryGuess {

    @NonNull private final ItemCategory category;
    @NonNull private final String cue;

    public CategoryGuess(@NonNull ItemCategory category, @NonNull String cue) {
        this.category = Objects.requireNonNull(category, "category");
        this.cue = Objects.requireNonNull(cue, "cue");
    }

    @NonNull
    public ItemCategory getCategory() {
        return category;
    }

    /** The words found on the label, for example "Drug Facts". The form shows them. */
    @NonNull
    public String getCue() {
        return cue;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CategoryGuess)) {
            return false;
        }
        CategoryGuess that = (CategoryGuess) other;
        return category == that.category && cue.equals(that.cue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, cue);
    }

    @NonNull
    @Override
    public String toString() {
        return "CategoryGuess{" + category + ", cue=" + cue + "}";
    }
}

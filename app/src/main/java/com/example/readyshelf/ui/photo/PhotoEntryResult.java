package com.example.readyshelf.ui.photo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readyshelf.domain.CategoryGuess;
import com.example.readyshelf.domain.CategoryGuesser;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.NameCandidates;
import com.example.readyshelf.domain.PhotoDateChoices;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * What photo entry hands back to the Add batch form (SPEC.md, "Photo entry"): the date the
 * household tapped (or none), lines of the product photo that could be the name, and a category
 * clue. It is never saved by itself. The form shows it, and the household checks everything
 * before saving.
 *
 * <p>Only the household's own choice of date is in here; the form never picks one. The name
 * lines are only suggestions to tap: the text on a photo can't tell which product it is.
 *
 * <p>This is plain Java, so it can be tested without a phone. An object of this class never
 * changes.
 */
public final class PhotoEntryResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Nullable private final PhotoDateChoices.Choice date;
    @NonNull private final ArrayList<String> nameLines;
    @Nullable private final ItemCategory category;
    @Nullable private final String categoryCue;

    private PhotoEntryResult(@Nullable PhotoDateChoices.Choice date, @NonNull List<String> nameLines,
                             @Nullable CategoryGuess categoryGuess) {
        this.date = date;
        this.nameLines = new ArrayList<>(nameLines);
        this.category = categoryGuess == null ? null : categoryGuess.getCategory();
        this.categoryCue = categoryGuess == null ? null : categoryGuess.getCue();
    }

    /**
     * The result of one photo session.
     *
     * @param chosenDate   the date the household tapped, or null for "Continue without a date"
     * @param productLines the text lines of the product photo (empty when there was none)
     * @param dateLines    the text lines of the date photo (empty when there was none)
     */
    @NonNull
    public static PhotoEntryResult of(@Nullable PhotoDateChoices.Choice chosenDate,
                                      @NonNull List<String> productLines,
                                      @NonNull List<String> dateLines) {
        // A clue such as "Drug Facts" can be on either photo, so both are searched.
        List<String> allLines = new ArrayList<>(productLines);
        allLines.addAll(dateLines);
        // Name suggestions come only from the product photo: a close photo of a date has no name.
        return new PhotoEntryResult(chosenDate, NameCandidates.pick(productLines),
                CategoryGuesser.guess(allLines));
    }

    /** The date the household tapped, or null when no date was chosen. */
    @Nullable
    public PhotoDateChoices.Choice getDate() {
        return date;
    }

    /** Lines of the product photo that could be the name. The household taps one, or none. */
    @NonNull
    public List<String> getNameLines() {
        return Collections.unmodifiableList(nameLines);
    }

    /** The category clue found on the photos, or null when there was none. */
    @Nullable
    public CategoryGuess getCategoryGuess() {
        return category == null || categoryCue == null ? null : new CategoryGuess(category, categoryCue);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PhotoEntryResult)) {
            return false;
        }
        PhotoEntryResult that = (PhotoEntryResult) other;
        return Objects.equals(date, that.date) && nameLines.equals(that.nameLines)
                && category == that.category && Objects.equals(categoryCue, that.categoryCue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, nameLines, category, categoryCue);
    }
}

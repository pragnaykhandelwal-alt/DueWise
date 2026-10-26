package com.example.readyshelf.ui.photo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.readyshelf.domain.DateCandidate;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.PhotoDateChoices;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for what photo entry hands back to the batch form. (The non-date cases of the old
 * LabelReadingTest moved here on 2026-10-12.)
 */
public class PhotoEntryResultTest {

    private static final List<String> NONE = Collections.emptyList();

    /** A choice made from a preset candidate (a fake parser answer, not a parser rule). */
    private static PhotoDateChoices.Choice presetChoice() {
        DateCandidate candidate = DateCandidate.complete("BEST BY 10/25/2026", "10/25/2026",
                DateLabel.BEST_BY, LocalDate.of(2026, 10, 25));
        return PhotoDateChoices.find((lines, today) -> Collections.singletonList(candidate),
                Collections.singletonList("text"), LocalDate.of(2026, 10, 12)).getChoices().get(0);
    }

    @Test
    public void combinesTheChosenDate_theCategoryClue_andTheNames() {
        PhotoDateChoices.Choice date = presetChoice();

        PhotoEntryResult result = PhotoEntryResult.of(date,
                Arrays.asList("NATURE VALLEY", "Nutrition Facts"), Arrays.asList("BEST BY 10/25/2026"));

        assertSame(date, result.getDate());
        assertEquals(ItemCategory.FOOD, result.getCategoryGuess().getCategory());
        assertEquals(Arrays.asList("Nature Valley"), result.getNameLines());
    }

    @Test
    public void namesComeOnlyFromTheProductPhoto() {
        PhotoEntryResult result = PhotoEntryResult.of(null,
                Arrays.asList("CRUNCHY GRANOLA BARS"), Arrays.asList("Nature Valley"));

        assertEquals(Arrays.asList("Crunchy Granola Bars"), result.getNameLines());
    }

    @Test
    public void aCategoryClueOnTheDatePhotoCounts() {
        PhotoEntryResult result = PhotoEntryResult.of(null,
                Arrays.asList("Pain Relief"), Arrays.asList("Drug Facts", "EXP 03/2027"));

        assertEquals(ItemCategory.MEDICINE, result.getCategoryGuess().getCategory());
        assertEquals("DRUG FACTS", result.getCategoryGuess().getCue());
    }

    @Test
    public void withoutAChosenDate_theDateIsNull() {
        assertNull(PhotoEntryResult.of(null, Arrays.asList("NATURE VALLEY"), NONE).getDate());
    }

    @Test
    public void nothingUseful_givesNoNamesAndNoCategory() {
        PhotoEntryResult result = PhotoEntryResult.of(null, Arrays.asList("", "~~"), NONE);

        assertTrue(result.getNameLines().isEmpty());
        assertNull(result.getCategoryGuess());
    }

    @Test
    public void theNameLinesCantBeChanged() {
        PhotoEntryResult result = PhotoEntryResult.of(null, Arrays.asList("NATURE VALLEY"), NONE);

        try {
            result.getNameLines().clear();
            fail("The name lines could be changed");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, result.getNameLines().size());
        }
    }

    @Test
    public void survivesBeingSaved() throws IOException, ClassNotFoundException {
        PhotoEntryResult result = PhotoEntryResult.of(presetChoice(),
                Arrays.asList("NATURE VALLEY", "Nutrition Facts"), Arrays.asList("BEST BY 10/25/2026"));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(result);
        }
        Object copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = in.readObject();
        }

        assertEquals(result, copy);
    }
}

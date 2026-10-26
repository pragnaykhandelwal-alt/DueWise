package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Tests for picking the label lines that could be the item's name. */
public class NameCandidatesTest {

    @Test
    public void keepsNameLines_inOrder_withOrdinaryCapitals() {
        List<String> names = NameCandidates.pick(Arrays.asList(
                "NATURE VALLEY", "CRUNCHY GRANOLA BARS", "Oats 'n Honey"));

        assertEquals(Arrays.asList("Nature Valley", "Crunchy Granola Bars", "Oats 'n Honey"), names);
    }

    @Test
    public void dropsDatesNutritionRowsAndNumbers() {
        List<String> names = NameCandidates.pick(Arrays.asList(
                "BEST BY 10/25/2026", "Nutrition Facts", "Calories 190", "12 BARS - 8.94 OZ",
                "0 16000 27547 8", "Total Fat 7g", "Granola Bars"));

        assertEquals(Arrays.asList("Granola Bars"), names);
    }

    @Test
    public void dropsLinesThatAreTooShortOrTooLong() {
        List<String> names = NameCandidates.pick(Arrays.asList(
                "OZ", "A very long line of small print that nobody would type as a name"));

        assertEquals(new ArrayList<String>(), names);
    }

    @Test
    public void dottedPrintReadAsRepeatedLetters_isDropped() {
        // These lines are what Tesseract read from a synthetic dot-matrix test picture on the phone.
        List<String> names = NameCandidates.pick(Arrays.asList(
                "ageees ge ggeegees", "geeg ceeeg gh gees, gees, gees. cones", "eeed ole eecdee",
                "100 TABLETS", "Peanut Butter", "Coffee"));

        assertEquals(Arrays.asList("100 Tablets", "Peanut Butter", "Coffee"), names);
    }

    @Test
    public void repeatsAreRemoved_ignoringCapitals() {
        assertEquals(Arrays.asList("Granola Bars"),
                NameCandidates.pick(Arrays.asList("GRANOLA BARS", "Granola Bars")));
    }

    @Test
    public void atMostEightLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lines.add("Name line " + (char) ('a' + i));
        }

        assertEquals(NameCandidates.MAX_LINES, NameCandidates.pick(lines).size());
    }

    @Test
    public void niceCase_onlyChangesAllCapitalLines() {
        assertEquals("Nature's Path", NameCandidates.niceCase("NATURE'S PATH"));
        assertEquals("Spring-Water", NameCandidates.niceCase("SPRING-WATER"));
        assertEquals("Oats 'n Honey", NameCandidates.niceCase("Oats 'n Honey"));
    }
}

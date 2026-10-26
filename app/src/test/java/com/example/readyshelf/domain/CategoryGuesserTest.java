package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;

/** Tests for guessing the category from label words. The label text is typed here. */
public class CategoryGuesserTest {

    @Test
    public void drugFacts_isMedicine() {
        CategoryGuess guess = guess("Drug Facts", "Active ingredient (in each tablet)");

        assertEquals(ItemCategory.MEDICINE, guess.getCategory());
        assertEquals("DRUG FACTS", guess.getCue());
    }

    @Test
    public void inactiveIngredients_isMedicine_notFood() {
        assertEquals(ItemCategory.MEDICINE, guess("Inactive ingredients: starch").getCategory());
    }

    @Test
    public void aMedicineCueAnywhere_winsOverFoodCues() {
        assertEquals(ItemCategory.MEDICINE,
                guess("Ingredients: sugar", "Calories 10", "Drug Facts").getCategory());
    }

    @Test
    public void tabletsAlone_isMedicine() {
        assertEquals(ItemCategory.MEDICINE, guess("100 TABLETS").getCategory());
    }

    @Test
    public void supplements_areOther_evenWithCapsules() {
        assertEquals(ItemCategory.OTHER, guess("Supplement Facts", "60 CAPSULES").getCategory());
    }

    @Test
    public void firstAidSupplies() {
        assertEquals(ItemCategory.FIRST_AID_SUPPLY, guess("ADHESIVE BANDAGES").getCategory());
        assertEquals(ItemCategory.FIRST_AID_SUPPLY, guess("STERILE GAUZE PADS").getCategory());
    }

    @Test
    public void bottledWater_isWater_evenWithNutritionFacts() {
        assertEquals(ItemCategory.WATER, guess("NATURAL SPRING WATER", "Nutrition Facts").getCategory());
    }

    @Test
    public void waterInAnIngredientList_isFood() {
        assertEquals(ItemCategory.FOOD, guess("INGREDIENTS: BEANS, WATER, SALT").getCategory());
        assertEquals(ItemCategory.FOOD, guess("Ingredients: purified water, sugar").getCategory());
    }

    @Test
    public void nutritionFacts_isFood() {
        assertEquals(ItemCategory.FOOD, guess("Nutrition Facts", "Serving size 1 bar").getCategory());
    }

    @Test
    public void batteries_areOther() {
        assertEquals(ItemCategory.OTHER, guess("AA ALKALINE BATTERIES").getCategory());
    }

    @Test
    public void noCue_givesNoGuess() {
        assertNull(CategoryGuesser.guess(Arrays.asList("NATURE VALLEY", "HANDCRAFTED GRANOLA")));
    }

    private static CategoryGuess guess(String... lines) {
        return CategoryGuesser.guess(Arrays.asList(lines));
    }
}

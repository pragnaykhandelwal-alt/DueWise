package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guesses the category of an item from the words on its label.
 *
 * <p>The cues are checked from top to bottom, and the first one found wins. Medicine cues come
 * first, so a medicine label is never read as food. (For example, "INACTIVE INGREDIENTS" also
 * contains the food cue "INGREDIENTS".) This matters because medicine is never swapped and
 * never gets a replacement need.
 *
 * <p>The household checks the guess before saving, and the form never moves a batch out of
 * Medicine because of a guess.
 */
public final class CategoryGuesser {

    private static final List<Cue> CUES = new ArrayList<>();

    static {
        // Medicine: the headings required on drug labels, and other drug-only words.
        cue("DRUG\\s*FACTS", ItemCategory.MEDICINE);
        cue("(IN)?ACTIVE\\s*INGREDIENTS?", ItemCategory.MEDICINE);
        cue("RX\\s*ONLY", ItemCategory.MEDICINE);
        cue("NDC", ItemCategory.MEDICINE);
        // Supplements go to Other, so they are never called medicine. Other can be swapped
        // (since 2026-10-22); the household checks the guess.
        cue("SUPPLEMENT\\s*FACTS", ItemCategory.OTHER);
        cue("TABLETS|CAPSULES|CAPLETS|SOFTGELS", ItemCategory.MEDICINE);
        cue("BANDAGES?|GAUZE|STERILE\\s*PADS?|FIRST\\s*AID", ItemCategory.FIRST_AID_SUPPLY);
        // Water: only clear water names, and never on an ingredient line (see guess()).
        cue("(SPRING|DRINKING|PURIFIED|DISTILLED|MINERAL)\\s*WATER", ItemCategory.WATER);
        cue("NUTRITION\\s*FACTS|INGREDIENTS?|CALORIES|SERVING\\s*SIZE", ItemCategory.FOOD);
        cue("BATTERY|BATTERIES|ALKALINE", ItemCategory.OTHER);
    }

    private CategoryGuesser() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Returns the category the label's words point to, or null when no cue was found.
     *
     * @param lines the text of the label photo, top to bottom
     */
    @Nullable
    public static CategoryGuess guess(@NonNull List<String> lines) {
        for (Cue cue : CUES) {
            for (String line : lines) {
                String upper = line.toUpperCase(Locale.ROOT);
                if (cue.category == ItemCategory.WATER && upper.contains("INGREDIENT")) {
                    continue; // "Ingredients: beans, water" is not bottled water
                }
                Matcher matcher = cue.pattern.matcher(upper);
                if (matcher.find()) {
                    String words = matcher.group().replaceAll("\\s+", " ");
                    return new CategoryGuess(cue.category, words);
                }
            }
        }
        return null;
    }

    private static void cue(String regex, ItemCategory category) {
        // "Not a letter before or after", so "NDC" doesn't match inside a longer word.
        CUES.add(new Cue(Pattern.compile("(?<![A-Z])(" + regex + ")(?![A-Z])"), category));
    }

    private static final class Cue {
        final Pattern pattern;
        final ItemCategory category;

        Cue(Pattern pattern, ItemCategory category) {
            this.pattern = pattern;
            this.category = category;
        }
    }
}

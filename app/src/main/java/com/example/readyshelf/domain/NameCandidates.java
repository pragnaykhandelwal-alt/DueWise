package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Picks the lines of a label photo that could be the item's name, such as "Crunchy Granola Bars".
 * The household taps one to use it. Nothing becomes the name without a tap.
 */
public final class NameCandidates {

    /** At most this many lines are offered. */
    public static final int MAX_LINES = 8;

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 40;

    // Lines with these words are label details, not names.
    private static final Pattern NOT_A_NAME = Pattern.compile("(?<![A-Z])("
            + "NUTRITION|SERVINGS?|CALORIES|TOTAL\\s*FAT|SODIUM|PROTEIN|CARBOHYDRATES?|DAILY\\s*VALUE"
            + "|INGREDIENTS?|DRUG\\s*FACTS|DIRECTIONS|WARNINGS?|KEEP\\s*OUT|BEST\\s*BY|USE\\s*BY"
            + "|EXP|LOT|NET\\s*WT|NET\\s*WEIGHT|DISTRIBUTED|MANUFACTURED|WWW|HTTPS?"
            + ")(?![A-Z])");

    // A number group with a separator, as in dates and codes: "10/12", "2026-10".
    private static final Pattern DATE_LIKE = Pattern.compile("[0-9]{1,4}[/.\\-][0-9]{1,2}");

    private NameCandidates() {
        // Only static methods, so no objects of this class are made.
    }

    /** Up to {@link #MAX_LINES} name-like lines, in the order they appear on the label. */
    @NonNull
    public static List<String> pick(@NonNull List<String> lines) {
        Map<String, String> picked = new LinkedHashMap<>(); // lowercase key -> line as shown
        for (String line : lines) {
            String clean = line.trim().replaceAll("\\s+", " ");
            if (!looksLikeAName(clean)) {
                continue;
            }
            String shown = niceCase(clean);
            String key = shown.toLowerCase(Locale.ROOT);
            if (!picked.containsKey(key)) {
                picked.put(key, shown);
            }
            if (picked.size() == MAX_LINES) {
                break;
            }
        }
        return new ArrayList<>(picked.values());
    }

    private static boolean looksLikeAName(String line) {
        if (line.length() < MIN_LENGTH || line.length() > MAX_LENGTH) {
            return false;
        }
        int letters = 0;
        int digits = 0;
        int others = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
            } else if (Character.isDigit(c)) {
                digits++;
            } else if (c != ' ') {
                others++;
            }
        }
        boolean enoughLetters = letters >= 3 && letters * 2 >= letters + digits + others;
        boolean fewDigits = digits * 2 <= letters;
        String upper = line.toUpperCase(Locale.ROOT);
        return enoughLetters && fewDigits
                && hasVarietyOfLetters(upper, letters)
                && !DATE_LIKE.matcher(line).find()
                && !NOT_A_NAME.matcher(upper).find();
    }

    /**
     * OCR sometimes reads a dotted (dot-matrix) print as rows of the same few letters, such as
     * "ageees ge ggeegees". Real names use more different letters, so a line of 6 or more letters
     * must use at least 2 different letters for every 5 letters.
     */
    private static boolean hasVarietyOfLetters(String upper, int letters) {
        if (letters < 6) {
            return true;
        }
        boolean[] seen = new boolean[26];
        int different = 0;
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c >= 'A' && c <= 'Z' && !seen[c - 'A']) {
                seen[c - 'A'] = true;
                different++;
            }
        }
        return different * 5 >= letters * 2;
    }

    /**
     * Turns an all-capitals line into ordinary capitals: "CRUNCHY GRANOLA BARS" becomes
     * "Crunchy Granola Bars". A line that already has small letters stays as it is.
     */
    @NonNull
    static String niceCase(@NonNull String text) {
        if (!text.equals(text.toUpperCase(Locale.ROOT))) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        boolean startOfWord = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                result.append(startOfWord ? c : Character.toLowerCase(c));
                startOfWord = false;
            } else {
                result.append(c);
                startOfWord = c == ' ' || c == '-' || c == '/';
            }
        }
        return result.toString();
    }
}

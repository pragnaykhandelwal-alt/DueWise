package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads one month and year from the printed-date text typed on the batch form, for the form's
 * "Use the last day of the month" button (SPEC.md, "Date entry").
 *
 * <p><b>Not for label photos.</b> Claude Code wrote this class on 2026-10-04 as LabelDateFinder,
 * which also found dates in label photos. Since 2026-10-12 possible dates in photos come only from
 * the student's date-candidate parser (DateCandidateParserContract). That parser must not call
 * this class. Its only caller is DateEntry.lastDayOfPrintedMonth.
 *
 * <p>To decide whether the text is exactly one month and year, it looks at every date shape in
 * the text:
 * <ul>
 *   <li>Formats: MM/DD/YY(YY), YYYY-MM-DD, YYYY-MMM(-DD), DD MMM YY(YY), MMM DD YY(YY), and the
 *       month-only forms MM/YYYY, YYYY-MM, MMM YYYY. "/", "-" and "." all work as separators.</li>
 *   <li>Two-part numbers such as "10/26" are never used: they could be a month and a year, or a
 *       month and a day.</li>
 *   <li>Numbers that could be month/day or day/month (04/05/2026) give two readings.</li>
 *   <li>LOT, PACKED, MFG and similar words mean the date after them is skipped.</li>
 * </ul>
 *
 * <p>Only Java features that also exist on Android 8 (API 26) are used.
 */
public final class PrintedMonthReader {

    // Month names as printed on packages. Longer names come first, so "JUNE" is found before "JUN".
    private static final String MONTHS = "(JANUARY|JAN|FEBRUARY|FEB|MARCH|MAR|APRIL|APR|MAY|JUNE|JUN"
            + "|JULY|JUL|AUGUST|AUG|SEPTEMBER|SEPT|SEP|OCTOBER|OCT|NOVEMBER|NOV|DECEMBER|DEC)";

    // "Not a letter before" and "not a letter after", so "BB" doesn't match inside "BBQ".
    private static final String NO_LETTER_BEFORE = "(?<![A-Z])";
    private static final String NO_LETTER_AFTER = "(?![A-Z])";
    private static final String NO_DIGIT_BEFORE = "(?<![0-9])";
    private static final String NO_DIGIT_AFTER = "(?![0-9])";

    private static final List<Keyword> KEYWORDS = new ArrayList<>();
    private static final List<DateShape> SHAPES = new ArrayList<>();

    static {
        // The kind of date that each keyword introduces.
        keyword("USE\\s*OR\\s*FREEZE\\s*BY", DateLabel.USE_BY);
        keyword("BEST\\s*(IF\\s*USED\\s*)?(BY|BEFORE|BEF)", DateLabel.BEST_BY);
        keyword("BBE?", DateLabel.BEST_BY);
        keyword("USE\\s*(BY|BEFORE)", DateLabel.USE_BY);
        keyword("EXP(IRES|IRY|IRATION)?", DateLabel.EXPIRES);
        keyword("(SELL|FREEZE)\\s*BY", DateLabel.OTHER);
        // Words for dates that are not a "best by" kind of date. A date right after them is skipped.
        keyword("(LOT|PACKED|PACK|PKD|MFG|MFD|MANUFACTURED|PRODUCED|PROD)", null);

        // Date shapes, tried in this order. A later shape can't reuse text an earlier one already used.
        shape(DateShape.YEAR_MONTH_DAY,
                NO_DIGIT_BEFORE + "(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})" + NO_DIGIT_AFTER);
        shape(DateShape.YEAR_MONTHNAME_DAY,
                NO_DIGIT_BEFORE + "(\\d{4})[-/. ]?" + MONTHS + NO_LETTER_AFTER
                        + "\\.?[-/. ]?(\\d{1,2})" + NO_DIGIT_AFTER);
        shape(DateShape.NUMBERS_3,
                NO_DIGIT_BEFORE + "(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4}|\\d{2})" + NO_DIGIT_AFTER);
        shape(DateShape.DAY_MONTHNAME_YEAR,
                NO_DIGIT_BEFORE + "(\\d{1,2})[-/. ]?" + MONTHS + NO_LETTER_AFTER
                        + "\\.?[-/., ]*(\\d{4}|\\d{2})" + NO_DIGIT_AFTER);
        shape(DateShape.MONTHNAME_DAY_YEAR,
                NO_LETTER_BEFORE + MONTHS + NO_LETTER_AFTER + "\\.?[-/. ]*(\\d{1,2})(?:ST|ND|RD|TH)?"
                        + NO_DIGIT_AFTER + "[-/., ]*(\\d{4}|\\d{2})" + NO_DIGIT_AFTER);
        shape(DateShape.YEAR_MONTHNAME,
                NO_DIGIT_BEFORE + "(\\d{4})[-/. ]?" + MONTHS + NO_LETTER_AFTER);
        shape(DateShape.MONTHNAME_YEAR,
                NO_LETTER_BEFORE + MONTHS + NO_LETTER_AFTER + "\\.?[-/. ]*(\\d{4})" + NO_DIGIT_AFTER);
        shape(DateShape.MONTH_YEAR,
                "(?<![0-9/.\\-])(\\d{1,2})[-/.](\\d{4})(?![0-9/.\\-])");
        shape(DateShape.YEAR_MONTH,
                "(?<![0-9/.\\-])(\\d{4})[-/.](\\d{1,2})(?![0-9/.\\-])");
    }

    private PrintedMonthReader() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * The month and year in text that shows only a month and year, such as "03/2027", "2027-03",
     * "MAR 2027", or "EXP 03/2027". Null when the text shows anything else: a full date, a
     * two-part number such as "10/26", more than one date, or no date. Any year is accepted,
     * because the household typed the text.
     */
    @Nullable
    public static YearMonth printedMonth(@NonNull String text) {
        List<Found> found = find(Collections.singletonList(text), LocalDate.MIN, LocalDate.MAX);
        if (found.size() != 1 || !found.get(0).monthOnly) {
            return null;
        }
        return YearMonth.from(found.get(0).date);
    }

    /** Finds the dates in {@code lines} from {@code earliest} to {@code latest}. */
    @NonNull
    private static List<Found> find(@NonNull List<String> lines, @NonNull LocalDate earliest,
                                    @NonNull LocalDate latest) {
        List<Found> found = new ArrayList<>();
        KeywordHit carriedOver = null; // a keyword at the end of the line before, not used there

        for (String rawLine : lines) {
            String line = fixDigits(rawLine.toUpperCase(Locale.ROOT));
            List<KeywordHit> keywordHits = findKeywords(line);
            List<DateHit> dateHits = findDates(line);

            KeywordHit waiting = carriedOver;
            int nextKeyword = 0;
            for (DateHit dateHit : dateHits) {
                // Use the closest keyword before this date that no other date has used yet.
                while (nextKeyword < keywordHits.size() && keywordHits.get(nextKeyword).end <= dateHit.start) {
                    waiting = keywordHits.get(nextKeyword);
                    nextKeyword++;
                }
                KeywordHit keyword = waiting;
                waiting = null; // a keyword belongs to the first date after it
                if (keyword != null && keyword.skipsDate) {
                    continue; // for example "LOT 2026-10-12" or "MFG 01/02/2025"
                }
                DateLabel label = keyword == null ? null : keyword.label;
                for (Found date : dateHit.readings(label)) {
                    if (!date.date.isBefore(earliest) && !date.date.isAfter(latest)) {
                        found.add(date);
                    }
                }
            }
            // A keyword after the last date on this line (or on a line with no date) can belong to
            // the first date on the next line. It never carries further than one line.
            carriedOver = nextKeyword < keywordHits.size()
                    ? keywordHits.get(keywordHits.size() - 1)
                    : null;
        }
        return withoutDuplicates(found);
    }

    /**
     * Fixes letters that OCR often reads instead of digits (O for 0; I, L and | for 1), but only
     * next to a digit and never inside a word, so "1O/I2/2O26" becomes "10/12/2026" while "OCT" stays.
     */
    @NonNull
    static String fixDigits(@NonNull String upper) {
        StringBuilder fixed = new StringBuilder(upper);
        for (int i = 0; i < fixed.length(); i++) {
            char c = fixed.charAt(i);
            if (c != 'O' && c != 'I' && c != 'L' && c != '|') {
                continue;
            }
            char before = i > 0 ? fixed.charAt(i - 1) : ' ';
            char after = i + 1 < fixed.length() ? fixed.charAt(i + 1) : ' ';
            boolean nextToDigit = Character.isDigit(before) || Character.isDigit(after);
            boolean insideWord = Character.isLetter(before) || Character.isLetter(after);
            if (nextToDigit && !insideWord) {
                fixed.setCharAt(i, c == 'O' ? '0' : '1');
            }
        }
        return fixed.toString();
    }

    private static List<KeywordHit> findKeywords(String line) {
        List<KeywordHit> hits = new ArrayList<>();
        for (Keyword keyword : KEYWORDS) {
            Matcher matcher = keyword.pattern.matcher(line);
            while (matcher.find()) {
                hits.add(new KeywordHit(matcher.start(), matcher.end(), keyword.label, keyword.skipsDate));
            }
        }
        // Keep the longest of overlapping keywords: "USE OR FREEZE BY" wins over "FREEZE BY".
        List<KeywordHit> kept = new ArrayList<>();
        for (KeywordHit hit : hits) {
            boolean insideLonger = false;
            for (KeywordHit other : hits) {
                boolean longer = other.end - other.start > hit.end - hit.start;
                if (other != hit && longer && other.start <= hit.start && hit.end <= other.end) {
                    insideLonger = true;
                    break;
                }
            }
            if (!insideLonger) {
                kept.add(hit);
            }
        }
        kept.sort((first, second) -> Integer.compare(first.start, second.start));
        return kept;
    }

    private static List<DateHit> findDates(String line) {
        List<DateHit> hits = new ArrayList<>();
        boolean[] used = new boolean[line.length()];
        for (DateShape shape : SHAPES) {
            Matcher matcher = shape.pattern.matcher(line);
            while (matcher.find()) {
                if (anyUsed(used, matcher.start(), matcher.end())) {
                    continue;
                }
                DateHit hit = shape.read(matcher);
                if (hit != null) {
                    for (int i = matcher.start(); i < matcher.end(); i++) {
                        used[i] = true;
                    }
                    hits.add(hit);
                }
            }
        }
        hits.sort((first, second) -> Integer.compare(first.start, second.start));
        return hits;
    }

    private static boolean anyUsed(boolean[] used, int start, int end) {
        for (int i = start; i < end; i++) {
            if (used[i]) {
                return true;
            }
        }
        return false;
    }

    /** Removes repeats of the same date and label, then puts dates with a keyword first. */
    private static List<Found> withoutDuplicates(List<Found> found) {
        Map<String, Found> unique = new LinkedHashMap<>();
        for (Found date : found) {
            String key = date.date + "|" + date.label + "|" + date.monthOnly;
            if (!unique.containsKey(key)) {
                unique.put(key, date);
            }
        }
        List<Found> result = new ArrayList<>();
        for (Found date : unique.values()) {
            if (date.label != null) {
                result.add(date);
            }
        }
        for (Found date : unique.values()) {
            if (date.label == null) {
                result.add(date);
            }
        }
        return result;
    }

    private static void keyword(String regex, @Nullable DateLabel label) {
        KEYWORDS.add(new Keyword(Pattern.compile(NO_LETTER_BEFORE + regex + NO_LETTER_AFTER),
                label, label == null));
    }

    private static void shape(int kind, String regex) {
        SHAPES.add(new DateShape(kind, Pattern.compile(regex)));
    }

    // 2-digit years are read as 20YY.
    private static int fullYear(String digits) {
        int year = Integer.parseInt(digits);
        return digits.length() == 2 ? 2000 + year : year;
    }

    private static int monthNumber(String name) {
        String start = name.substring(0, 3);
        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(start)) {
                return i + 1;
            }
        }
        return 0;
    }

    /** A real calendar date, or null (for example February 30). */
    @Nullable
    private static LocalDate dateOrNull(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException notADate) {
            return null;
        }
    }

    /** The last day of a month, or null when the month isn't real. */
    @Nullable
    private static LocalDate endOfMonthOrNull(int year, int month) {
        try {
            return YearMonth.of(year, month).atEndOfMonth();
        } catch (DateTimeException notAMonth) {
            return null;
        }
    }

    private static final class Keyword {
        final Pattern pattern;
        @Nullable final DateLabel label;
        final boolean skipsDate;

        Keyword(Pattern pattern, @Nullable DateLabel label, boolean skipsDate) {
            this.pattern = pattern;
            this.label = label;
            this.skipsDate = skipsDate;
        }
    }

    private static final class KeywordHit {
        final int start;
        final int end;
        @Nullable final DateLabel label;
        final boolean skipsDate;

        KeywordHit(int start, int end, @Nullable DateLabel label, boolean skipsDate) {
            this.start = start;
            this.end = end;
            this.label = label;
            this.skipsDate = skipsDate;
        }
    }

    /**
     * One reading of a date found in the text. A month-only date is kept as the last day of its
     * month, so two month-only dates of the same month count as one.
     */
    private static final class Found {
        final LocalDate date;
        @Nullable final DateLabel label;
        final boolean monthOnly;

        Found(LocalDate date, @Nullable DateLabel label, boolean monthOnly) {
            this.date = date;
            this.label = label;
            this.monthOnly = monthOnly;
        }
    }

    /** A date found in one line: one reading, or two when day and month could be swapped. */
    private static final class DateHit {
        final int start;
        final LocalDate first;
        @Nullable final LocalDate second; // the day-first reading of an ambiguous date
        final boolean monthOnly;

        DateHit(int start, LocalDate first, @Nullable LocalDate second, boolean monthOnly) {
            this.start = start;
            this.first = first;
            this.second = second;
            this.monthOnly = monthOnly;
        }

        List<Found> readings(@Nullable DateLabel label) {
            List<Found> readings = new ArrayList<>();
            readings.add(new Found(first, label, monthOnly));
            if (second != null) {
                readings.add(new Found(second, label, monthOnly));
            }
            return readings;
        }
    }

    /** One way of printing a date, and how to turn the matched text into a date. */
    private static final class DateShape {
        static final int YEAR_MONTH_DAY = 1;
        static final int YEAR_MONTHNAME_DAY = 2;
        static final int NUMBERS_3 = 3;
        static final int DAY_MONTHNAME_YEAR = 4;
        static final int MONTHNAME_DAY_YEAR = 5;
        static final int YEAR_MONTHNAME = 6;
        static final int MONTHNAME_YEAR = 7;
        static final int MONTH_YEAR = 8;
        static final int YEAR_MONTH = 9;

        final int kind;
        final Pattern pattern;

        DateShape(int kind, Pattern pattern) {
            this.kind = kind;
            this.pattern = pattern;
        }

        /** Turns a match into a DateHit, or null when the numbers are not a real date. */
        @Nullable
        DateHit read(Matcher m) {
            int start = m.start();
            switch (kind) {
                case YEAR_MONTH_DAY:
                    return single(start, dateOrNull(fullYear(m.group(1)),
                            Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))), false);
                case YEAR_MONTHNAME_DAY:
                    return single(start, dateOrNull(fullYear(m.group(1)),
                            monthNumber(m.group(2)), Integer.parseInt(m.group(3))), false);
                case NUMBERS_3:
                    return numbers(start, Integer.parseInt(m.group(1)),
                            Integer.parseInt(m.group(2)), fullYear(m.group(3)));
                case DAY_MONTHNAME_YEAR:
                    return single(start, dateOrNull(fullYear(m.group(3)),
                            monthNumber(m.group(2)), Integer.parseInt(m.group(1))), false);
                case MONTHNAME_DAY_YEAR:
                    return single(start, dateOrNull(fullYear(m.group(3)),
                            monthNumber(m.group(1)), Integer.parseInt(m.group(2))), false);
                case YEAR_MONTHNAME:
                    return single(start, endOfMonthOrNull(fullYear(m.group(1)),
                            monthNumber(m.group(2))), true);
                case MONTHNAME_YEAR:
                    return single(start, endOfMonthOrNull(fullYear(m.group(2)),
                            monthNumber(m.group(1))), true);
                case MONTH_YEAR:
                    return single(start, endOfMonthOrNull(fullYear(m.group(2)),
                            Integer.parseInt(m.group(1))), true);
                case YEAR_MONTH:
                    return single(start, endOfMonthOrNull(fullYear(m.group(1)),
                            Integer.parseInt(m.group(2))), true);
                default:
                    return null;
            }
        }

        @Nullable
        private static DateHit single(int start, @Nullable LocalDate date, boolean monthOnly) {
            return date == null ? null : new DateHit(start, date, null, monthOnly);
        }

        /** "A/B/YEAR": month/day (US) or day/month. Both readings are kept when both are possible. */
        @Nullable
        private static DateHit numbers(int start, int a, int b, int year) {
            LocalDate monthFirst = dateOrNull(year, a, b);
            LocalDate dayFirst = dateOrNull(year, b, a);
            if (monthFirst != null && dayFirst != null && !monthFirst.equals(dayFirst)) {
                return new DateHit(start, monthFirst, dayFirst, false);
            }
            LocalDate onlyReading = monthFirst != null ? monthFirst : dayFirst;
            return single(start, onlyReading, false);
        }
    }
}

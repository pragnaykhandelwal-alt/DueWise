package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The date-candidate rules of photo entry: finds the possible printed dates in the text of a date
 * photo (DateCandidateParserContract; SPEC.md, "DateCandidateParser requirements").
 *
 * <p>Claude Code wrote this class on 2026-10-18 at the student's request (CLAUDE.md rules 1 and
 * 2), after the student approved its tests (DateCandidateParserTest) and the decisions D1 to D11
 * in SPEC.md. See AI-USE.md.
 *
 * <p>How each line is read:
 * <ul>
 *   <li>A reading copy is made in capital letters, with the letters that the text reader often
 *       reads instead of digits fixed (D4). It has the same length as the line, so the date text
 *       of each candidate is cut from the printed line, unchanged.</li>
 *   <li>Label words (D9) give the label of the first date after them, on the same line or, from
 *       the end of a line, on the next line (D8). A date right after LOT is a lot code and is
 *       never offered (D10).</li>
 *   <li>Date shapes are tried in the order of {@link Shape}: full dates with numbers or month
 *       names (D2, D6), then the month-and-year shapes. Two-part numbers such as "10/26" are never
 *       used (D7), impossible dates are skipped (D5), and two-digit years are 20YY (D3).</li>
 *   <li>Numbers that could be month/day or day/month give one TWO_READINGS candidate. Numbers
 *       that can be read only one way give a COMPLETE one (D1).</li>
 * </ul>
 * The candidates come back in printed order: top to bottom, then left to right.
 *
 * <p>PrintedMonthReader knows the same shapes, but the rules must not call it (SPEC.md), so they
 * are written again here. Only Java features that also exist on Android 8 (API 26) are used.
 */
public final class DateCandidateParser implements DateCandidateParserContract {

    // Month names as printed on packages. Longer names come first, so "JUNE" is found before "JUN".
    private static final String MONTHS = "(JANUARY|JAN|FEBRUARY|FEB|MARCH|MAR|APRIL|APR|MAY|JUNE|JUN"
            + "|JULY|JUL|AUGUST|AUG|SEPTEMBER|SEPT|SEP|OCTOBER|OCT|NOVEMBER|NOV|DECEMBER|DEC)";
    private static final String[] MONTH_STARTS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL",
            "AUG", "SEP", "OCT", "NOV", "DEC"};

    // "Not a letter before" and "not a letter after", so "BB" doesn't match inside "BBQ".
    private static final String NO_LETTER_BEFORE = "(?<![A-Z])";
    private static final String NO_LETTER_AFTER = "(?![A-Z])";
    private static final String NO_DIGIT_BEFORE = "(?<![0-9])";
    private static final String NO_DIGIT_AFTER = "(?![0-9])";

    /** The label words (D9). LOT gives no label: the date right after it is a lot code. */
    private static final List<LabelWords> LABEL_WORDS = Collections.unmodifiableList(Arrays.asList(
            new LabelWords("USE\\s*OR\\s*FREEZE\\s*BY", DateLabel.USE_BY),
            new LabelWords("BEST\\s*(IF\\s*USED\\s*)?(BY|BEFORE|BEF)", DateLabel.BEST_BY),
            new LabelWords("BBE?", DateLabel.BEST_BY),
            new LabelWords("USE\\s*(BY|BEFORE)", DateLabel.USE_BY),
            new LabelWords("EXP(IRES|IRY|IRATION)?", DateLabel.EXPIRES),
            new LabelWords("(SELL|FREEZE)\\s*BY", DateLabel.OTHER),
            new LabelWords("(MFG|MFD|MANUFACTURED|PACKED|PKD|PRODUCED|PROD)", DateLabel.OTHER),
            new LabelWords("LOT", null)));

    /** The date shapes, tried in this order. A later shape can't reuse text that an earlier one used. */
    private enum Shape {
        /** 2027-03-15 */
        YEAR_MONTH_DAY(NO_DIGIT_BEFORE + "(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})" + NO_DIGIT_AFTER),
        /** 2027-MAR-15 */
        YEAR_MONTHNAME_DAY(NO_DIGIT_BEFORE + "(\\d{4})[-/. ]?" + MONTHS + NO_LETTER_AFTER
                + "\\.?[-/. ]?(\\d{1,2})" + NO_DIGIT_AFTER),
        /** 10/25/2026, 13/01/2027, 03/04/27: month first or day first */
        NUMBERS(NO_DIGIT_BEFORE + "(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4}|\\d{2})" + NO_DIGIT_AFTER),
        /** 12 OCT 2026, 12OCT26 */
        DAY_MONTHNAME_YEAR(NO_DIGIT_BEFORE + "(\\d{1,2})[-/. ]?" + MONTHS + NO_LETTER_AFTER
                + "\\.?[-/., ]*(\\d{4}|\\d{2})" + NO_DIGIT_AFTER),
        /** OCT 12, 2026 and JUNE 1ST 2027 */
        MONTHNAME_DAY_YEAR(NO_LETTER_BEFORE + MONTHS + NO_LETTER_AFTER
                + "\\.?[-/. ]*(\\d{1,2})(?:ST|ND|RD|TH)?" + NO_DIGIT_AFTER
                + "[-/., ]*(\\d{4}|\\d{2})" + NO_DIGIT_AFTER),
        /** 2027 MAR: a month and year */
        YEAR_MONTHNAME(NO_DIGIT_BEFORE + "(\\d{4})[-/. ]?" + MONTHS + NO_LETTER_AFTER),
        /** MAR 2027: a month and year */
        MONTHNAME_YEAR(NO_LETTER_BEFORE + MONTHS + NO_LETTER_AFTER + "\\.?[-/. ]*(\\d{4})"
                + NO_DIGIT_AFTER),
        /** 03/2027: a month and year */
        MONTH_YEAR("(?<![0-9/.\\-])(\\d{1,2})[-/.](\\d{4})(?![0-9/.\\-])"),
        /** 2027-03: a month and year */
        YEAR_MONTH("(?<![0-9/.\\-])(\\d{4})[-/.](\\d{1,2})(?![0-9/.\\-])");

        final Pattern pattern;

        Shape(String regex) {
            pattern = Pattern.compile(regex);
        }
    }

    /** Holds no data that changes, so one object can be shared by every screen and thread. */
    public DateCandidateParser() {
    }

    @NonNull
    @Override
    public List<DateCandidate> findCandidates(@NonNull List<String> lines, @NonNull LocalDate today) {
        // today is not used (D11): the same text gives the same dates on every day, and old or
        // far-future dates are still offered.
        List<DateCandidate> candidates = new ArrayList<>();
        LabelHit carriedOver = null; // label words at the end of the line before, not used there

        for (String line : lines) {
            String reading = readingCopy(line);
            List<LabelHit> labelHits = findLabelWords(reading);

            LabelHit waiting = carriedOver;
            int nextLabel = 0;
            for (DateHit date : findDates(reading)) {
                // Use the closest label words before this date that no other date has used yet.
                while (nextLabel < labelHits.size() && labelHits.get(nextLabel).end <= date.start) {
                    waiting = labelHits.get(nextLabel);
                    nextLabel++;
                }
                LabelHit words = waiting;
                waiting = null; // label words belong only to the first date after them
                if (words != null && words.lotCode) {
                    continue; // for example "LOT 2026-10-28": a lot code is not a date
                }
                candidates.add(date.toCandidate(line, words == null ? null : words.label));
            }

            // Label words at the end of this line that no date here used can belong to the first
            // date on the next line (D8). They never carry further than one line.
            LabelHit last = labelHits.isEmpty() ? null : labelHits.get(labelHits.size() - 1);
            carriedOver = last != null && nextLabel < labelHits.size()
                    && onlyPunctuationAfter(reading, last.end) ? last : null;
        }
        return candidates;
    }

    /**
     * The line in capital letters, with the letters that the text reader often reads instead of
     * digits fixed (D4): O as 0, and I, L or | as 1, but only next to a digit and never inside a
     * word, so "1O/2I/2O26" becomes "10/21/2026" while "OCT" stays. Every character keeps its
     * place, so a date found in this copy can be cut from the printed line.
     */
    @NonNull
    private static String readingCopy(@NonNull String line) {
        char[] copy = new char[line.length()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = Character.toUpperCase(line.charAt(i));
        }
        for (int i = 0; i < copy.length; i++) {
            char c = copy[i];
            if (c != 'O' && c != 'I' && c != 'L' && c != '|') {
                continue;
            }
            char before = i > 0 ? copy[i - 1] : ' ';
            char after = i + 1 < copy.length ? copy[i + 1] : ' ';
            boolean nextToDigit = Character.isDigit(before) || Character.isDigit(after);
            boolean insideWord = Character.isLetter(before) || Character.isLetter(after);
            if (nextToDigit && !insideWord) {
                copy[i] = c == 'O' ? '0' : '1';
            }
        }
        return new String(copy);
    }

    /** The label words in one reading copy, from left to right. */
    @NonNull
    private static List<LabelHit> findLabelWords(@NonNull String reading) {
        List<LabelHit> hits = new ArrayList<>();
        for (LabelWords words : LABEL_WORDS) {
            Matcher matcher = words.pattern.matcher(reading);
            while (matcher.find()) {
                hits.add(new LabelHit(matcher.start(), matcher.end(), words.label));
            }
        }
        // Keep the longest of overlapping label words: "USE OR FREEZE BY" wins over "FREEZE BY".
        List<LabelHit> kept = new ArrayList<>();
        for (LabelHit hit : hits) {
            if (!insideLonger(hit, hits)) {
                kept.add(hit);
            }
        }
        Collections.sort(kept, (first, second) -> Integer.compare(first.start, second.start));
        return kept;
    }

    private static boolean insideLonger(@NonNull LabelHit hit, @NonNull List<LabelHit> hits) {
        for (LabelHit other : hits) {
            boolean longer = other.end - other.start > hit.end - hit.start;
            if (other != hit && longer && other.start <= hit.start && hit.end <= other.end) {
                return true;
            }
        }
        return false;
    }

    /** True when nothing but spaces or punctuation comes after {@code from} in the line. */
    private static boolean onlyPunctuationAfter(@NonNull String reading, int from) {
        for (int i = from; i < reading.length(); i++) {
            if (Character.isLetterOrDigit(reading.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** The dates in one reading copy, from left to right. */
    @NonNull
    private static List<DateHit> findDates(@NonNull String reading) {
        List<DateHit> hits = new ArrayList<>();
        boolean[] used = new boolean[reading.length()];
        for (Shape shape : Shape.values()) {
            Matcher matcher = shape.pattern.matcher(reading);
            while (matcher.find()) {
                if (anyUsed(used, matcher.start(), matcher.end())) {
                    continue;
                }
                DateHit hit = read(shape, matcher);
                if (hit != null) {
                    Arrays.fill(used, matcher.start(), matcher.end(), true);
                    hits.add(hit);
                }
            }
        }
        Collections.sort(hits, (first, second) -> Integer.compare(first.start, second.start));
        return hits;
    }

    private static boolean anyUsed(@NonNull boolean[] used, int start, int end) {
        for (int i = start; i < end; i++) {
            if (used[i]) {
                return true;
            }
        }
        return false;
    }

    /** Turns a match into a date, or null when the numbers are not a real date (D5). */
    @Nullable
    private static DateHit read(@NonNull Shape shape, @NonNull Matcher m) {
        switch (shape) {
            case YEAR_MONTH_DAY:
                return complete(m, dateOrNull(year(m.group(1)), number(m.group(2)),
                        number(m.group(3))));
            case YEAR_MONTHNAME_DAY:
                return complete(m, dateOrNull(year(m.group(1)), monthNumber(m.group(2)),
                        number(m.group(3))));
            case NUMBERS:
                return numbers(m, number(m.group(1)), number(m.group(2)), year(m.group(3)));
            case DAY_MONTHNAME_YEAR:
                return complete(m, dateOrNull(year(m.group(3)), monthNumber(m.group(2)),
                        number(m.group(1))));
            case MONTHNAME_DAY_YEAR:
                return complete(m, dateOrNull(year(m.group(3)), monthNumber(m.group(1)),
                        number(m.group(2))));
            case YEAR_MONTHNAME:
                return monthAndYear(m, monthOrNull(year(m.group(1)), monthNumber(m.group(2))));
            case MONTHNAME_YEAR:
                return monthAndYear(m, monthOrNull(year(m.group(2)), monthNumber(m.group(1))));
            case MONTH_YEAR:
                return monthAndYear(m, monthOrNull(year(m.group(2)), number(m.group(1))));
            default: // YEAR_MONTH
                return monthAndYear(m, monthOrNull(year(m.group(1)), number(m.group(2))));
        }
    }

    /**
     * "A/B/YEAR" is month/day (as in the US) or day/month. When both readings are real, different
     * dates, both are kept (TWO_READINGS). Otherwise the one real reading is the date (D1).
     */
    @Nullable
    private static DateHit numbers(@NonNull Matcher m, int first, int second, int year) {
        LocalDate monthFirst = dateOrNull(year, first, second);
        LocalDate dayFirst = dateOrNull(year, second, first);
        if (monthFirst != null && dayFirst != null && !monthFirst.equals(dayFirst)) {
            return new DateHit(m.start(), m.end(), DateCandidate.Kind.TWO_READINGS, monthFirst,
                    dayFirst, null);
        }
        return complete(m, monthFirst != null ? monthFirst : dayFirst);
    }

    @Nullable
    private static DateHit complete(@NonNull Matcher m, @Nullable LocalDate date) {
        return date == null ? null
                : new DateHit(m.start(), m.end(), DateCandidate.Kind.COMPLETE, date, null, null);
    }

    @Nullable
    private static DateHit monthAndYear(@NonNull Matcher m, @Nullable YearMonth month) {
        return month == null ? null
                : new DateHit(m.start(), m.end(), DateCandidate.Kind.MONTH_AND_YEAR, null, null,
                        month);
    }

    // Two-digit years are read as 20YY (D3).
    private static int year(@NonNull String digits) {
        int year = Integer.parseInt(digits);
        return digits.length() == 2 ? 2000 + year : year;
    }

    private static int number(@NonNull String digits) {
        return Integer.parseInt(digits);
    }

    /** 1 for JAN or JANUARY, up to 12 for DEC or DECEMBER. */
    private static int monthNumber(@NonNull String name) {
        String start = name.substring(0, 3);
        for (int i = 0; i < MONTH_STARTS.length; i++) {
            if (MONTH_STARTS[i].equals(start)) {
                return i + 1;
            }
        }
        return 0; // not a month, so dateOrNull and monthOrNull give null
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

    /** A real month, or null (for example month 13). */
    @Nullable
    private static YearMonth monthOrNull(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException notAMonth) {
            return null;
        }
    }

    /** One kind of label words and the label they give. A null label means LOT: a lot code. */
    private static final class LabelWords {
        final Pattern pattern;
        @Nullable final DateLabel label;

        LabelWords(@NonNull String regex, @Nullable DateLabel label) {
            this.pattern = Pattern.compile(NO_LETTER_BEFORE + regex + NO_LETTER_AFTER);
            this.label = label;
        }
    }

    /** Label words found in one line. */
    private static final class LabelHit {
        final int start;
        final int end;
        @Nullable final DateLabel label;
        final boolean lotCode;

        LabelHit(int start, int end, @Nullable DateLabel label) {
            this.start = start;
            this.end = end;
            this.label = label;
            this.lotCode = label == null;
        }
    }

    /** A date found in one line, before its label is known. */
    private static final class DateHit {
        final int start;
        final int end;
        @NonNull final DateCandidate.Kind kind;
        @Nullable final LocalDate date; // COMPLETE: the date; TWO_READINGS: the month-first reading
        @Nullable final LocalDate dayFirst; // TWO_READINGS only
        @Nullable final YearMonth month; // MONTH_AND_YEAR only

        DateHit(int start, int end, @NonNull DateCandidate.Kind kind, @Nullable LocalDate date,
                @Nullable LocalDate dayFirst, @Nullable YearMonth month) {
            this.start = start;
            this.end = end;
            this.kind = kind;
            this.date = date;
            this.dayFirst = dayFirst;
            this.month = month;
        }

        /** The candidate: the whole printed line is the raw text, the matched part the date text. */
        @NonNull
        DateCandidate toCandidate(@NonNull String line, @Nullable DateLabel label) {
            String dateText = line.substring(start, end);
            switch (kind) {
                case COMPLETE:
                    return DateCandidate.complete(line, dateText, label, date);
                case TWO_READINGS:
                    return DateCandidate.twoReadings(line, dateText, label, date, dayFirst);
                default: // MONTH_AND_YEAR
                    return DateCandidate.monthAndYear(line, dateText, label, month);
            }
        }
    }
}

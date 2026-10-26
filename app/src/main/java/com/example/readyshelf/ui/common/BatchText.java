package com.example.readyshelf.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateEntry;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DateStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns batch details into the sentences the screens show. */
public final class BatchText {

    // Joins the parts of a product description, for example "Low sodium · 15 oz".
    private static final String PART_SEPARATOR = " · ";

    private BatchText() {
        // Only static methods, so no objects of this class are made.
    }

    /** "6 bars", or "6 (no unit entered)" when there is no unit. */
    @NonNull
    public static String amount(@NonNull Context context, int quantity, @NonNull String unit) {
        if (unit.isEmpty()) {
            return context.getString(R.string.amount_no_unit, quantity);
        }
        return context.getString(R.string.amount_with_unit, quantity, unit);
    }

    /**
     * "26 Oct 2026": the day, a month word, and the year, so a date can't be read two ways.
     * The month word follows the phone's language.
     */
    @NonNull
    public static String date(@NonNull LocalDate date) {
        return date(date, Locale.getDefault());
    }

    /** "26 Oct 2026" with the month word in the given language. */
    @NonNull
    public static String date(@NonNull LocalDate date, @NonNull Locale locale) {
        return DateTimeFormatter.ofPattern("d MMM uuuu", locale).format(date);
    }

    /** "Best by: 26 Oct 2026", or "No date entered". */
    @NonNull
    public static String labeledDate(@NonNull Context context, @Nullable LocalDate date,
                                     @NonNull DateLabel label) {
        if (date == null) {
            return context.getString(R.string.no_date_entered);
        }
        return context.getString(R.string.labeled_date,
                context.getString(DisplayNames.dateLabel(label)), date(date));
    }

    /**
     * The date as it was entered: "Best by: 26 Oct 2026", "Expires: “03/2027” (incomplete
     * date)", or "No date entered". It never says whether the date was confirmed.
     */
    @NonNull
    public static String dateEntry(@NonNull Context context, @Nullable LocalDate date,
                                   @Nullable String printedDateText, @NonNull DateLabel label) {
        switch (DateEntry.kindOf(date, printedDateText)) {
            case COMPLETE:
                return labeledDate(context, date, label);
            case INCOMPLETE:
                return context.getString(R.string.labeled_incomplete_date,
                        context.getString(DisplayNames.dateLabel(label)), printedDateText.trim());
            default:
                return context.getString(R.string.no_date_entered);
        }
    }

    /** The date of a batch as it was entered (see the method above). */
    @NonNull
    public static String dateEntry(@NonNull Context context, @NonNull BatchInfo batch) {
        return dateEntry(context, batch.getDate(), batch.getPrintedDateText(), batch.getDateLabel());
    }

    /**
     * Why a batch shows "Date needs review", for example "Date needs review: the date and
     * label type are not confirmed yet." Null when the date doesn't need review.
     */
    @Nullable
    public static String dateReview(@NonNull Context context, @Nullable LocalDate date,
                                    @Nullable String printedDateText, boolean confirmed) {
        if (!DateEntry.needsReview(date, confirmed)) {
            return null;
        }
        switch (DateEntry.kindOf(date, printedDateText)) {
            case COMPLETE:
                return context.getString(R.string.date_review_unconfirmed);
            case INCOMPLETE:
                return context.getString(R.string.date_review_incomplete);
            default:
                return context.getString(R.string.date_review_missing);
        }
    }

    /** Why this batch shows "Date needs review", or null when it doesn't. */
    @Nullable
    public static String dateReview(@NonNull Context context, @NonNull BatchInfo batch) {
        return dateReview(context, batch.getDate(), batch.getPrintedDateText(),
                batch.isDateConfirmed());
    }

    /** "Low sodium · 15 oz": the variant and package size that were entered, or "". */
    @NonNull
    public static String productDetails(@NonNull String variant, @NonNull String packageSize) {
        List<String> parts = new ArrayList<>();
        if (!variant.isEmpty()) {
            parts.add(variant);
        }
        if (!packageSize.isEmpty()) {
            parts.add(packageSize);
        }
        return String.join(PART_SEPARATOR, parts);
    }

    /**
     * One product on one line, with its variant, package size, and unit always shown:
     * "Canned beans — Low sodium · 15 oz · unit: cans", or
     * "Canned beans — no variant · no package size · unit: cans".
     */
    @NonNull
    public static String productChoice(@NonNull Context context, @NonNull String name,
                                       @NonNull String variant, @NonNull String packageSize,
                                       @NonNull String unit) {
        return context.getString(R.string.product_choice, name,
                productChoiceDetails(context, variant, packageSize, unit));
    }

    /** "Low sodium · 15 oz · unit: cans", or "no variant · no package size · unit: cans". */
    @NonNull
    public static String productChoiceDetails(@NonNull Context context, @NonNull String variant,
                                              @NonNull String packageSize, @NonNull String unit) {
        return String.join(PART_SEPARATOR,
                variant.isEmpty() ? context.getString(R.string.product_no_variant) : variant,
                packageSize.isEmpty() ? context.getString(R.string.product_no_size) : packageSize,
                unit.isEmpty() ? context.getString(R.string.product_no_unit)
                        : context.getString(R.string.product_unit, unit));
    }

    /** The product of a batch on one line (see the method above). */
    @NonNull
    public static String productChoice(@NonNull Context context, @NonNull BatchInfo batch) {
        return productChoice(context, batch.getName(), batch.getVariant(), batch.getPackageSize(),
                batch.getUnit());
    }

    /** "Variant: Low sodium. Package size: 15 oz. Unit: cans." with "none" for missing values. */
    @NonNull
    public static String productFacts(@NonNull Context context, @NonNull String variant,
                                      @NonNull String packageSize, @NonNull String unit) {
        String none = context.getString(R.string.product_value_none);
        return context.getString(R.string.product_facts,
                variant.isEmpty() ? none : variant,
                packageSize.isEmpty() ? none : packageSize,
                unit.isEmpty() ? none : unit);
    }

    /** "6 bars in Earthquake kit" */
    @NonNull
    public static String amountInPlace(@NonNull Context context, @NonNull BatchInfo batch) {
        return context.getString(R.string.amount_in_place,
                amount(context, batch.getQuantity(), batch.getUnit()), batch.getLocationName());
    }

    /** "Granola bars, 6 bars in Earthquake kit, Best by: 26 Oct 2026" */
    @NonNull
    public static String summary(@NonNull Context context, @NonNull BatchInfo batch) {
        return context.getString(R.string.batch_summary, batch.getName(),
                amountInPlace(context, batch), dateEntry(context, batch));
    }

    /** "Status: Review soon" */
    @NonNull
    public static String statusLine(@NonNull Context context, @NonNull DateStatus status) {
        return context.getString(R.string.status_line, context.getString(DisplayNames.status(status)));
    }

    /** "26 Oct 2026, 3:15 PM", in the phone's time zone. */
    @NonNull
    public static String dateTime(long epochMillis) {
        ZonedDateTime time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault());
        return date(time.toLocalDate()) + ", "
                + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time);
    }
}

package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Search, filters, and date order for the Inventory screen.
 * This is plain Java, so it can be tested without a phone.
 */
public final class InventoryFilter {

    /** The two date orders the Inventory screen offers. */
    public enum DateOrder {
        SOONEST_FIRST,
        LATEST_FIRST
    }

    private InventoryFilter() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Returns the batches that match, in the chosen date order.
     *
     * @param searchText text to look for in the product name or variant (capital letters
     *                   don't matter); null or blank matches every batch
     * @param locationId keep only this location, or null for all locations
     * @param category   keep only this category, or null for all categories
     * @param order      the date order; batches without a date come last in both orders
     */
    @NonNull
    public static List<BatchInfo> apply(@NonNull List<BatchInfo> batches,
                                        @Nullable String searchText,
                                        @Nullable Long locationId,
                                        @Nullable ItemCategory category,
                                        @NonNull DateOrder order) {
        String wanted = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        List<BatchInfo> result = new ArrayList<>();
        for (BatchInfo batch : batches) {
            boolean nameMatches = wanted.isEmpty()
                    || batch.getName().toLowerCase(Locale.ROOT).contains(wanted)
                    || batch.getVariant().toLowerCase(Locale.ROOT).contains(wanted);
            boolean locationMatches = locationId == null || batch.getLocationId() == locationId;
            boolean categoryMatches = category == null || batch.getCategory() == category;
            if (nameMatches && locationMatches && categoryMatches) {
                result.add(batch);
            }
        }
        result.sort(byDate(order));
        return result;
    }

    /**
     * Keeps the batches that have the chosen status, in the same order. It runs after
     * {@link #apply}, because a batch's status comes from the date rules, not from the batch.
     *
     * @param statuses the status of each batch, by batch id. A batch without an entry has no
     *                 status: archived batches are never given to the date rules.
     * @param wanted   keep only this status, or null for all statuses (then batches without a
     *                 status are kept too)
     */
    @NonNull
    public static List<BatchInfo> withStatus(@NonNull List<BatchInfo> batches,
                                             @NonNull Map<Long, DateStatus> statuses,
                                             @Nullable DateStatus wanted) {
        List<BatchInfo> result = new ArrayList<>();
        for (BatchInfo batch : batches) {
            if (wanted == null || statuses.get(batch.getId()) == wanted) {
                result.add(batch);
            }
        }
        return result;
    }

    /**
     * Compares batches by date in the given order. Batches without a date come last.
     * Batches with the same date are sorted by name, then by id.
     */
    @NonNull
    public static Comparator<BatchInfo> byDate(@NonNull DateOrder order) {
        Comparator<LocalDate> dates = order == DateOrder.SOONEST_FIRST
                ? Comparator.<LocalDate>naturalOrder()
                : Comparator.<LocalDate>reverseOrder();
        return Comparator.comparing(BatchInfo::getDate, Comparator.nullsLast(dates))
                .thenComparing(BatchInfo::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(BatchInfo::getId);
    }
}

package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The replacement needs added up for each product and place: the amount that a need's "Add to
 * purchase list" adds, and the "Needed now" amount of an item on the list. This is plain Java,
 * so it can be tested without a phone.
 *
 * <p>The product and the place together are a total's key. The key stays the same while the
 * amounts change, and the purchase list can hold at most one item for each key. Totals are made
 * again from the current needs whenever stock changes; nothing about them is stored.
 *
 * <p>Until 2026-10-22 the Purchase list showed these totals as their own "Suggestions" section,
 * and only for food in kits. The student removed that section; the names "suggestion" and "kit"
 * stay in the code (the place is stored as the purchase item's kitLocationId).
 */
public final class PurchaseSuggestions {

    /** The key of a total: one product for one place. An object of this class never changes. */
    public static final class Key {
        private final long productId;
        private final long kitLocationId;

        public Key(long productId, long kitLocationId) {
            this.productId = productId;
            this.kitLocationId = kitLocationId;
        }

        public long getProductId() {
            return productId;
        }

        public long getKitLocationId() {
            return kitLocationId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key)) {
                return false;
            }
            Key that = (Key) other;
            return productId == that.productId && kitLocationId == that.kitLocationId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, kitLocationId);
        }

        @NonNull
        @Override
        public String toString() {
            return "Key{product=" + productId + ", kit=" + kitLocationId + "}";
        }
    }

    /** One total: how much of one product to buy for one place. Never changes. */
    public static final class Suggestion {
        @NonNull private final Key key;
        @NonNull private final List<RuleRunner.ShownNeed> needs;
        private final int quantity;

        Suggestion(@NonNull Key key, @NonNull List<RuleRunner.ShownNeed> needs, int quantity) {
            this.key = key;
            this.needs = Collections.unmodifiableList(new ArrayList<>(needs));
            this.quantity = quantity;
        }

        @NonNull
        public Key getKey() {
            return key;
        }

        /** The needs this suggestion adds up, in the order they were given (at least one). */
        @NonNull
        public List<RuleRunner.ShownNeed> getNeeds() {
            return needs;
        }

        /**
         * The first batch of the suggestion: it gives the product's name, variant, size, and
         * unit, and the place's name. Every batch of a total has the same product and place.
         */
        @NonNull
        public BatchInfo getFirstBatch() {
            return needs.get(0).getBatch();
        }

        /** How many to buy: the sum of the needs, at most {@link StockChange#MAX_QUANTITY}. */
        public int getQuantity() {
            return quantity;
        }

        /** True for made-up sample data. Sample suggestions can't be added to the list. */
        public boolean isSample() {
            return getFirstBatch().isSample();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Suggestion)) {
                return false;
            }
            Suggestion that = (Suggestion) other;
            return quantity == that.quantity && key.equals(that.key) && needs.equals(that.needs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, needs, quantity);
        }

        @NonNull
        @Override
        public String toString() {
            return "Suggestion{" + key + ", quantity=" + quantity + "}";
        }
    }

    /** How a suggestion compares with the purchase list. */
    public enum ListState {
        /** The list has no item with this key. */
        NOT_ON_LIST,
        /** The list has an item with this key and the same amount. */
        ON_LIST_SAME,
        /** The list has an item with this key, but a different amount. */
        ON_LIST_OTHER_QUANTITY
    }

    private static final Comparator<Suggestion> BY_NAME_THEN_KIT = Comparator
            .comparing((Suggestion s) -> s.getFirstBatch().getName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(s -> s.getFirstBatch().getLocationName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(s -> s.getKey().getProductId())
            .thenComparingLong(s -> s.getKey().getKitLocationId());

    private PurchaseSuggestions() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Adds up the needs for each product and place. RuleRunner decides which batches have a
     * need, so every need with an amount is used. The result is sorted by product name, then
     * place name.
     */
    @NonNull
    public static List<Suggestion> from(@NonNull List<RuleRunner.ShownNeed> needs) {
        Map<Key, List<RuleRunner.ShownNeed>> byKey = new LinkedHashMap<>();
        for (RuleRunner.ShownNeed need : needs) {
            BatchInfo batch = need.getBatch();
            if (need.getStillNeeded() < 1) {
                continue;
            }
            Key key = new Key(batch.getProductId(), batch.getLocationId());
            List<RuleRunner.ShownNeed> forKey = byKey.get(key);
            if (forKey == null) {
                forKey = new ArrayList<>();
                byKey.put(key, forKey);
            }
            forKey.add(need);
        }
        List<Suggestion> result = new ArrayList<>();
        for (Map.Entry<Key, List<RuleRunner.ShownNeed>> entry : byKey.entrySet()) {
            long total = 0;
            for (RuleRunner.ShownNeed need : entry.getValue()) {
                total += need.getStillNeeded();
            }
            int quantity = (int) Math.min(total, StockChange.MAX_QUANTITY);
            result.add(new Suggestion(entry.getKey(), entry.getValue(), quantity));
        }
        result.sort(BY_NAME_THEN_KIT);
        return result;
    }

    /**
     * How a suggestion compares with the purchase list.
     *
     * @param listedQuantities the amount of each item on the list, by key
     */
    @NonNull
    public static ListState stateOf(@NonNull Suggestion suggestion,
                                    @NonNull Map<Key, Integer> listedQuantities) {
        Integer listed = listedQuantities.get(suggestion.getKey());
        if (listed == null) {
            return ListState.NOT_ON_LIST;
        }
        return listed == suggestion.getQuantity() ? ListState.ON_LIST_SAME
                : ListState.ON_LIST_OTHER_QUANTITY;
    }
}

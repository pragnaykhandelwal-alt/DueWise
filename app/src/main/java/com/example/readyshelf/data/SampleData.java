package com.example.readyshelf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readyshelf.domain.ConditionAnswer;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.FoodClass;
import com.example.readyshelf.domain.FoodType;
import com.example.readyshelf.domain.ItemCategory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Made-up products and batches for the separate sample data (see DataSets). They were made only
 * when the household tapped "Open sample data" in Settings; that card was removed on 2026-10-22,
 * so only the tests make them now. They go only into the sample-data file, never into the
 * household's own records. Each one has sample = true, so the screens also mark it "Sample item".
 *
 * <p>Dates are counted from the day the sample data is opened ({@code today}), so the sample
 * always shows the same situations:
 * <ul>
 *   <li>Granola bars, one product counted in bars: 6 in the Emergency kit dated 20 days away,
 *       and 6 in the Pantry dated 240 days away. Both have a confirmed complete date, the label
 *       type Best by, the food class Shelf-stable food, and the food answers that swaps need.
 *       Once the student's swap rules are connected, they can propose a swap of these two.</li>
 *   <li>3 more granola bars in the Pantry with only a month and year printed. An incomplete date
 *       can't be confirmed, so this batch stays out of every swap.</li>
 *   <li>4 cans of beans in the Emergency kit dated 14 days away, with no beans in the Pantry to
 *       replace them.</li>
 *   <li>Medicine in the First aid box whose expiration date was 10 days ago. ReadyShelf only
 *       reports the date; it gives no advice about taking or replacing medicine.</li>
 * </ul>
 * The food answers are made-up records, like the ones a household enters. ReadyShelf does not
 * check or judge food.
 */
final class SampleData {

    /** One made-up batch and the made-up product it belongs to. */
    static final class Item {
        final Product product;
        final Batch batch;

        Item(Product product, Batch batch) {
            this.product = product;
            this.batch = batch;
        }
    }

    private SampleData() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Builds the sample items. Batches of the same product share one Product object, so the
     * repository adds that product only once and every such batch gets the same product id.
     *
     * @param today the phone's local date when the sample data is opened
     */
    @NonNull
    static List<Item> create(@NonNull LocalDate today, long pantryId, long kitId, long firstAidId) {
        Product bars = product("Granola bars", "Oat and honey", "1.5 oz", "bars",
                FoodClass.SHELF_STABLE_FOOD);
        Product beans = product("Canned beans", "Black beans", "15 oz", "cans",
                FoodClass.SHELF_STABLE_FOOD);
        Product tablets = product("Pain reliever tablets", "", "24 tablets", "bottles", null);

        List<Item> items = new ArrayList<>();
        items.add(food(bars, FoodType.DRY_OR_BOXED, 6, today.plusDays(20), null, kitId));
        items.add(food(bars, FoodType.DRY_OR_BOXED, 6, today.plusDays(240), null, pantryId));
        items.add(food(bars, FoodType.DRY_OR_BOXED, 3, null, monthAndYear(today.plusMonths(9)),
                pantryId));
        items.add(food(beans, FoodType.CANNED, 4, today.plusDays(14), null, kitId));
        items.add(medicine(tablets, 2, today.minusDays(10), firstAidId));
        return items;
    }

    /** What a label shows when only the month and year are printed, for example "07/2027". */
    @NonNull
    static String monthAndYear(@NonNull LocalDate date) {
        return String.format(Locale.ROOT, "%02d/%d", date.getMonthValue(), date.getYear());
    }

    private static Product product(String name, String variant, String packageSize, String unit,
                                   @Nullable FoodClass foodClass) {
        Product product = new Product();
        product.name = name;
        product.variant = variant;
        product.packageSize = packageSize;
        product.unit = unit;
        product.foodClass = foodClass;
        product.sample = true;
        return product;
    }

    /**
     * A food batch with either a complete date or only the printed text of an incomplete date.
     * Its made-up answers are the ones swaps need: sealed, shelf-stable, not damaged, and no
     * storage concern. Only a complete date counts as confirmed.
     */
    private static Item food(Product product, FoodType foodType, int quantity,
                             @Nullable LocalDate date, @Nullable String printedDateText,
                             long locationId) {
        Batch batch = batch(ItemCategory.FOOD, quantity, date, DateLabel.BEST_BY, locationId);
        batch.printedDateText = printedDateText;
        batch.foodType = foodType;
        batch.sealed = ConditionAnswer.YES;
        batch.shelfStable = ConditionAnswer.YES;
        batch.damaged = ConditionAnswer.NO;
        batch.storageConcern = ConditionAnswer.NO;
        return new Item(product, batch);
    }

    /** A medicine batch. Food answers are only for food, so it has none. */
    private static Item medicine(Product product, int quantity, LocalDate date, long locationId) {
        return new Item(product,
                batch(ItemCategory.MEDICINE, quantity, date, DateLabel.EXPIRES, locationId));
    }

    private static Batch batch(ItemCategory category, int quantity, @Nullable LocalDate date,
                               DateLabel dateLabel, long locationId) {
        Batch batch = new Batch();
        batch.category = category;
        batch.quantity = quantity;
        batch.date = date;
        batch.dateLabel = dateLabel;
        // Made-up complete dates count as confirmed; an incomplete date never does.
        batch.dateConfirmed = date != null;
        batch.locationId = locationId;
        batch.sample = true;
        return batch;
    }
}

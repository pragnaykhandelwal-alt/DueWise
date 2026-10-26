package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Ignore;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One batch together with its product and location. The screens show it, and the rules read it.
 *
 * <p>An object of this class never changes after it is made (it is "immutable").
 * Room fills it from the query in data/BatchDao, using the only constructor that is not marked
 * {@code @Ignore}. Each parameter name of that constructor must match a column name in that query.
 */
public final class BatchInfo {

    private final long id;
    private final long productId;
    @NonNull private final String name;
    @NonNull private final String variant;
    @NonNull private final String packageSize;
    @NonNull private final String unit;
    @Nullable private final FoodClass foodClass;
    @NonNull private final ItemCategory category;
    private final int quantity;
    @Nullable private final LocalDate date;
    @Nullable private final String printedDateText;
    @NonNull private final DateLabel dateLabel;
    private final boolean dateConfirmed;
    private final long locationId;
    @NonNull private final String locationName;
    @NonNull private final LocationType locationType;
    @Nullable private final FoodType foodType;
    @Nullable private final ConditionAnswer sealed;
    @Nullable private final ConditionAnswer shelfStable;
    @Nullable private final ConditionAnswer damaged;
    @Nullable private final ConditionAnswer storageConcern;
    private final boolean archived;
    private final boolean sample;

    public BatchInfo(long id, long productId, @NonNull String name, @NonNull String variant,
                     @NonNull String packageSize, @NonNull String unit,
                     @Nullable FoodClass foodClass,
                     @NonNull ItemCategory category, int quantity,
                     @Nullable LocalDate date, @Nullable String printedDateText,
                     @NonNull DateLabel dateLabel, boolean dateConfirmed,
                     long locationId, @NonNull String locationName,
                     @NonNull LocationType locationType,
                     @Nullable FoodType foodType, @Nullable ConditionAnswer sealed,
                     @Nullable ConditionAnswer shelfStable, @Nullable ConditionAnswer damaged,
                     @Nullable ConditionAnswer storageConcern,
                     boolean archived, boolean sample) {
        this.id = id;
        this.productId = productId;
        this.name = Objects.requireNonNull(name, "name");
        this.variant = Objects.requireNonNull(variant, "variant");
        this.packageSize = Objects.requireNonNull(packageSize, "packageSize");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.foodClass = foodClass;
        this.category = Objects.requireNonNull(category, "category");
        this.quantity = quantity;
        this.date = date;
        this.printedDateText = printedDateText;
        this.dateLabel = Objects.requireNonNull(dateLabel, "dateLabel");
        this.dateConfirmed = dateConfirmed;
        this.locationId = locationId;
        this.locationName = Objects.requireNonNull(locationName, "locationName");
        this.locationType = Objects.requireNonNull(locationType, "locationType");
        this.foodType = foodType;
        this.sealed = sealed;
        this.shelfStable = shelfStable;
        this.damaged = damaged;
        this.storageConcern = storageConcern;
        this.archived = archived;
        this.sample = sample;
    }

    /**
     * The same as the first constructor, for a product with no food class ("Not classified").
     * Tests written before database version 3 use it. (Room ignores this constructor.)
     */
    @Ignore
    public BatchInfo(long id, long productId, @NonNull String name, @NonNull String variant,
                     @NonNull String packageSize, @NonNull String unit,
                     @NonNull ItemCategory category, int quantity,
                     @Nullable LocalDate date, @Nullable String printedDateText,
                     @NonNull DateLabel dateLabel, boolean dateConfirmed,
                     long locationId, @NonNull String locationName,
                     @NonNull LocationType locationType,
                     @Nullable FoodType foodType, @Nullable ConditionAnswer sealed,
                     @Nullable ConditionAnswer shelfStable, @Nullable ConditionAnswer damaged,
                     @Nullable ConditionAnswer storageConcern,
                     boolean archived, boolean sample) {
        this(id, productId, name, variant, packageSize, unit, null, category, quantity, date,
                printedDateText, dateLabel, dateConfirmed, locationId, locationName, locationType,
                foodType, sealed, shelfStable, damaged, storageConcern, archived, sample);
    }

    /**
     * A shorter way to make a batch, mainly for tests. The other values are:
     * product id 0, no variant or package size, no food class, no printed date text, the date
     * counts as confirmed when there is one, no food type, every condition answer "Not checked"
     * (null), and not archived. (Room ignores this constructor.)
     */
    @Ignore
    public BatchInfo(long id, @NonNull String name, @NonNull ItemCategory category, int quantity,
                     @NonNull String unit, @Nullable LocalDate date, @NonNull DateLabel dateLabel,
                     long locationId, @NonNull String locationName,
                     @NonNull LocationType locationType, boolean sample) {
        this(id, 0, name, "", "", unit, null, category, quantity, date, null, dateLabel,
                date != null, locationId, locationName, locationType, null, null, null, null, null,
                false, sample);
    }

    public long getId() {
        return id;
    }

    /** The product this batch belongs to. */
    public long getProductId() {
        return productId;
    }

    /** The product name exactly as the household typed it, for example "Granola bars". */
    @NonNull
    public String getName() {
        return name;
    }

    /** The product's variant, for example "Low sodium". Empty when there is none. */
    @NonNull
    public String getVariant() {
        return variant;
    }

    /** The size printed on one package, for example "15 oz". Empty when it wasn't entered. */
    @NonNull
    public String getPackageSize() {
        return packageSize;
    }

    /**
     * The product's food class as the household chose it, or null for "Not classified".
     * It belongs to the product, so every batch of the product has the same one.
     */
    @Nullable
    public FoodClass getFoodClass() {
        return foodClass;
    }

    @NonNull
    public ItemCategory getCategory() {
        return category;
    }

    /** How many there are: a whole number from 0 to 9999. */
    public int getQuantity() {
        return quantity;
    }

    /** What the quantity counts, for example "cans". Empty when no unit was entered. */
    @NonNull
    public String getUnit() {
        return unit;
    }

    /** The complete date printed on the package, or null when there is no complete date. */
    @Nullable
    public LocalDate getDate() {
        return date;
    }

    /**
     * What the label shows when only part of a date is printed or readable, for example
     * "03/2027", exactly as the household typed it. Null otherwise.
     */
    @Nullable
    public String getPrintedDateText() {
        return printedDateText;
    }

    /** The kind of date printed on the package. */
    @NonNull
    public DateLabel getDateLabel() {
        return dateLabel;
    }

    /**
     * True when the household confirmed that the complete date and its label type match the
     * package. Always false when there is no complete date.
     */
    public boolean isDateConfirmed() {
        return dateConfirmed;
    }

    public long getLocationId() {
        return locationId;
    }

    @NonNull
    public String getLocationName() {
        return locationName;
    }

    @NonNull
    public LocationType getLocationType() {
        return locationType;
    }

    /** The kind of food package, or null when not chosen. Only food batches have one. */
    @Nullable
    public FoodType getFoodType() {
        return foodType;
    }

    /** "Sealed?" for a food batch: YES, NO, or null for "Not checked". */
    @Nullable
    public ConditionAnswer getSealed() {
        return sealed;
    }

    /** "Shelf-stable?" for a food batch: YES, NO, or null for "Not checked". */
    @Nullable
    public ConditionAnswer getShelfStable() {
        return shelfStable;
    }

    /** "Damaged?" for a food batch: YES, NO, or null for "Not checked". */
    @Nullable
    public ConditionAnswer getDamaged() {
        return damaged;
    }

    /** "Storage concern?" for a food batch: YES, NO, or null for "Not checked". */
    @Nullable
    public ConditionAnswer getStorageConcern() {
        return storageConcern;
    }

    /** True for an archived batch. The rules never see archived batches. */
    public boolean isArchived() {
        return archived;
    }

    /** True for made-up sample data. False for the household's own batches. */
    public boolean isSample() {
        return sample;
    }

    // equals and hashCode compare every field, so two BatchInfo objects with the
    // same values count as equal. Lists use this to see whether a row changed.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BatchInfo)) {
            return false;
        }
        BatchInfo that = (BatchInfo) other;
        return id == that.id
                && productId == that.productId
                && quantity == that.quantity
                && dateConfirmed == that.dateConfirmed
                && locationId == that.locationId
                && archived == that.archived
                && sample == that.sample
                && name.equals(that.name)
                && variant.equals(that.variant)
                && packageSize.equals(that.packageSize)
                && unit.equals(that.unit)
                && foodClass == that.foodClass
                && category == that.category
                && Objects.equals(date, that.date)
                && Objects.equals(printedDateText, that.printedDateText)
                && dateLabel == that.dateLabel
                && locationName.equals(that.locationName)
                && locationType == that.locationType
                && foodType == that.foodType
                && sealed == that.sealed
                && shelfStable == that.shelfStable
                && damaged == that.damaged
                && storageConcern == that.storageConcern;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId, name, variant, packageSize, unit, foodClass, category,
                quantity, date, printedDateText, dateLabel, dateConfirmed, locationId, locationName,
                locationType, foodType, sealed, shelfStable, damaged, storageConcern, archived,
                sample);
    }

    @NonNull
    @Override
    public String toString() {
        return "BatchInfo{id=" + id + ", name=" + name + ", foodClass=" + foodClass
                + ", quantity=" + quantity + ", date=" + date + ", location=" + locationName
                + ", archived=" + archived + ", sample=" + sample + "}";
    }
}

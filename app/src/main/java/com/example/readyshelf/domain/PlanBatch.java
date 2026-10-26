package com.example.readyshelf.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * One batch as the swap rules (the student's RotationPlanner) see it. An object of this class
 * never changes.
 *
 * <p>It only holds the recorded facts. Deciding which batches can be swapped is the job of
 * the swap rules.
 */
public final class PlanBatch {

    private final long batchId;
    private final long productId;
    @NonNull private final String unit;
    @NonNull private final ItemCategory category;
    @Nullable private final FoodClass foodClass;
    @NonNull private final LocationType locationType;
    private final int quantity;
    @NonNull private final DateInput date;
    @Nullable private final ConditionAnswer sealed;
    @Nullable private final ConditionAnswer damaged;
    @Nullable private final ConditionAnswer storageConcern;

    public PlanBatch(long batchId, long productId, @NonNull String unit,
                     @NonNull ItemCategory category, @Nullable FoodClass foodClass,
                     @NonNull LocationType locationType, int quantity, @NonNull DateInput date,
                     @Nullable ConditionAnswer sealed, @Nullable ConditionAnswer damaged,
                     @Nullable ConditionAnswer storageConcern) {
        this.batchId = batchId;
        this.productId = productId;
        this.unit = Objects.requireNonNull(unit, "unit");
        this.category = Objects.requireNonNull(category, "category");
        this.foodClass = foodClass;
        this.locationType = Objects.requireNonNull(locationType, "locationType");
        this.quantity = quantity;
        this.date = Objects.requireNonNull(date, "date");
        this.sealed = sealed;
        this.damaged = damaged;
        this.storageConcern = storageConcern;
    }

    /**
     * The plan input of one batch, with the product's stored food class (null means
     * "unclassified"). Since 2026-10-22 the swap rules use neither the class nor the condition
     * answers; they stay here so the contract doesn't change.
     */
    @NonNull
    public static PlanBatch from(@NonNull BatchInfo batch, @Nullable FoodClass foodClass) {
        return new PlanBatch(batch.getId(), batch.getProductId(), batch.getUnit(),
                batch.getCategory(), foodClass, batch.getLocationType(), batch.getQuantity(),
                DateInput.from(batch), batch.getSealed(), batch.getDamaged(),
                batch.getStorageConcern());
    }

    public long getBatchId() {
        return batchId;
    }

    public long getProductId() {
        return productId;
    }

    /** The counting unit of the product, for example "cans". Empty when none was entered. */
    @NonNull
    public String getUnit() {
        return unit;
    }

    @NonNull
    public ItemCategory getCategory() {
        return category;
    }

    /** The product's food class, or null when the product is unclassified. */
    @Nullable
    public FoodClass getFoodClass() {
        return foodClass;
    }

    /** The type of the place where the batch is kept, for example EMERGENCY_KIT or PANTRY. */
    @NonNull
    public LocationType getLocationType() {
        return locationType;
    }

    /** The available quantity, counted in the unit. */
    public int getQuantity() {
        return quantity;
    }

    @NonNull
    public DateInput getDate() {
        return date;
    }

    /** "Sealed?": YES, NO, or null for "Not checked". */
    @Nullable
    public ConditionAnswer getSealed() {
        return sealed;
    }

    /** "Damaged package?": YES, NO, or null for "Not checked". */
    @Nullable
    public ConditionAnswer getDamaged() {
        return damaged;
    }

    /** "Storage concern?": YES, NO, or null for "Not checked". */
    @Nullable
    public ConditionAnswer getStorageConcern() {
        return storageConcern;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlanBatch)) {
            return false;
        }
        PlanBatch that = (PlanBatch) other;
        return batchId == that.batchId
                && productId == that.productId
                && quantity == that.quantity
                && unit.equals(that.unit)
                && category == that.category
                && foodClass == that.foodClass
                && locationType == that.locationType
                && date.equals(that.date)
                && sealed == that.sealed
                && damaged == that.damaged
                && storageConcern == that.storageConcern;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, productId, unit, category, foodClass, locationType, quantity,
                date, sealed, damaged, storageConcern);
    }

    @NonNull
    @Override
    public String toString() {
        return "PlanBatch{id=" + batchId + ", product=" + productId + ", " + quantity + " " + unit
                + ", " + category + ", " + foodClass + ", " + locationType + ", " + date + "}";
    }
}

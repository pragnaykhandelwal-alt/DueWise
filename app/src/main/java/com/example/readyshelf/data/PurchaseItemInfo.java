package com.example.readyshelf.data;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * One purchase-list item together with its product and place, for the Purchase list screen.
 * An object of this class never changes.
 *
 * <p>Room fills it from the query in PurchaseItemDao. Each constructor parameter name must
 * match a column name in that query.
 */
public final class PurchaseItemInfo {

    private final long id;
    private final long productId;
    @NonNull private final String name;
    @NonNull private final String variant;
    @NonNull private final String packageSize;
    @NonNull private final String unit;
    private final long kitLocationId;
    @NonNull private final String locationName;
    private final int quantity;
    private final long addedAtMillis;

    public PurchaseItemInfo(long id, long productId, @NonNull String name, @NonNull String variant,
                            @NonNull String packageSize, @NonNull String unit, long kitLocationId,
                            @NonNull String locationName, int quantity, long addedAtMillis) {
        this.id = id;
        this.productId = productId;
        this.name = Objects.requireNonNull(name, "name");
        this.variant = Objects.requireNonNull(variant, "variant");
        this.packageSize = Objects.requireNonNull(packageSize, "packageSize");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.kitLocationId = kitLocationId;
        this.locationName = Objects.requireNonNull(locationName, "locationName");
        this.quantity = quantity;
        this.addedAtMillis = addedAtMillis;
    }

    public long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getVariant() {
        return variant;
    }

    @NonNull
    public String getPackageSize() {
        return packageSize;
    }

    @NonNull
    public String getUnit() {
        return unit;
    }

    /** The place that the purchase is for (a kit until 2026-10-22; now any place). */
    public long getKitLocationId() {
        return kitLocationId;
    }

    @NonNull
    public String getLocationName() {
        return locationName;
    }

    /** How many to buy. */
    public int getQuantity() {
        return quantity;
    }

    public long getAddedAtMillis() {
        return addedAtMillis;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PurchaseItemInfo)) {
            return false;
        }
        PurchaseItemInfo that = (PurchaseItemInfo) other;
        return id == that.id
                && productId == that.productId
                && kitLocationId == that.kitLocationId
                && quantity == that.quantity
                && addedAtMillis == that.addedAtMillis
                && name.equals(that.name)
                && variant.equals(that.variant)
                && packageSize.equals(that.packageSize)
                && unit.equals(that.unit)
                && locationName.equals(that.locationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId, name, variant, packageSize, unit, kitLocationId,
                locationName, quantity, addedAtMillis);
    }

    @NonNull
    @Override
    public String toString() {
        return "PurchaseItemInfo{id=" + id + ", name=" + name + ", quantity=" + quantity
                + ", kit=" + locationName + "}";
    }
}

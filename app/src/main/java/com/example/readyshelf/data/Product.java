package com.example.readyshelf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.readyshelf.domain.FoodClass;

/**
 * One row of the "products" table: one kind of item the household keeps, for example
 * "Canned beans, Low sodium, 15 oz, counted in cans". Every batch points to a product.
 *
 * <p>ReadyShelfRepository doesn't add a second product with the same name, variant,
 * package size, and unit (capital letters don't matter).
 */
@Entity(tableName = "products")
public class Product {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    /** For example "Low sodium". Empty when the product has no variant. */
    @NonNull
    public String variant = "";

    /** The size printed on one package, for example "15 oz". Empty when it wasn't entered. */
    @NonNull
    public String packageSize = "";

    /**
     * What quantities count, for example "cans". A new product must have one. A product made
     * from a version 1 batch that had no unit keeps an empty unit.
     */
    @NonNull
    public String unit = "";

    /** True for products made for the sample data. The form doesn't offer them. */
    public boolean sample;

    /**
     * What kind of food the household said this product is. Null means "Not classified".
     * Added in database version 3, so every older product starts as null. It was chosen on the
     * batch form and used by the swap rules until 2026-10-22; since then nothing asks, shows, or
     * uses it, but every product keeps it (no database change).
     */
    @Nullable
    public FoodClass foodClass;
}

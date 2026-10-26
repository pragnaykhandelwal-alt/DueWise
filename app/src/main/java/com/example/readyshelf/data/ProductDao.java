package com.example.readyshelf.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.readyshelf.domain.FoodClass;

import java.util.List;

/** Database access for products. Room writes the code for these methods. */
@Dao
public interface ProductDao {

    /** Every product (sample products too), sorted by name, variant, package size, and unit. */
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE, variant COLLATE NOCASE, "
            + "packageSize COLLATE NOCASE, unit COLLATE NOCASE, id")
    LiveData<List<Product>> observeAll();

    /** Every product (sample products too), oldest first, read now. Background thread only. */
    @Query("SELECT * FROM products ORDER BY id")
    List<Product> getAll();

    /**
     * The household's product with these details, ignoring capital letters, or null.
     * Sample products are left out. Background thread only.
     */
    @Nullable
    @Query("SELECT * FROM products WHERE sample = 0 AND name = :name COLLATE NOCASE "
            + "AND variant = :variant COLLATE NOCASE AND packageSize = :packageSize COLLATE NOCASE "
            + "AND unit = :unit COLLATE NOCASE ORDER BY id LIMIT 1")
    Product findSame(String name, String variant, String packageSize, String unit);

    /** One product, or null. Background thread only. */
    @Nullable
    @Query("SELECT * FROM products WHERE id = :productId")
    Product getById(long productId);

    /** Adds a product and returns its new id. Background thread only. */
    @Insert
    long insert(Product product);

    /**
     * Saves a product's food class (null for "Not classified"). Returns how many rows changed
     * (0 if the product is gone). Background thread only.
     */
    @Query("UPDATE products SET foodClass = :foodClass WHERE id = :productId")
    int setFoodClass(long productId, @Nullable FoodClass foodClass);

    /** Deletes the sample products that no batch uses any more. Background thread only. */
    @Query("DELETE FROM products WHERE sample = 1 AND id NOT IN (SELECT productId FROM batches)")
    int deleteUnusedSample();
}

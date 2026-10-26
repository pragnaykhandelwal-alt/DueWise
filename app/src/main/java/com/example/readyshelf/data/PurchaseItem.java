package com.example.readyshelf.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One row of the "purchase_items" table: an amount of one product that the household plans
 * to buy for one place. It is only a note: it is never counted as stock.
 *
 * <p>The product and the place together are the item's key. The unique index allows only one
 * item for each product and place, so the same need can't be added twice.
 *
 * <p>CASCADE: when a product is deleted (only "Remove sample data" deletes products), its
 * items are deleted with it. Locations can't be deleted.
 */
@Entity(tableName = "purchase_items",
        foreignKeys = {
                @ForeignKey(entity = Product.class,
                        parentColumns = "id",
                        childColumns = "productId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = StorageLocation.class,
                        parentColumns = "id",
                        childColumns = "kitLocationId")},
        indices = {@Index(value = {"productId", "kitLocationId"}, unique = true),
                @Index("kitLocationId")})
public class PurchaseItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long productId;

    /**
     * The place that the purchase is for. Until 2026-10-22 it was always a kit (Emergency kit
     * or First aid location); since then it can be any place. The column keeps its name, so
     * the database didn't change.
     */
    public long kitLocationId;

    /** How many to buy, counted in the product's unit: a whole number from 1 to 9999. */
    public int quantity;

    /** When it was added to the list, in milliseconds since January 1, 1970. */
    public long addedAtMillis;
}

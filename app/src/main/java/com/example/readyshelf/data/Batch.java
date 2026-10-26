package com.example.readyshelf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.readyshelf.domain.ConditionAnswer;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.FoodType;
import com.example.readyshelf.domain.ItemCategory;

import java.time.LocalDate;

/**
 * One row of the "batches" table: some of one product, with one date, in one location.
 *
 * <p>The foreign keys mean every batch must point to a real location and a real product.
 * A location or product that still has batches can't be deleted.
 */
@Entity(tableName = "batches",
        foreignKeys = {
                @ForeignKey(entity = StorageLocation.class,
                        parentColumns = "id",
                        childColumns = "locationId"),
                @ForeignKey(entity = Product.class,
                        parentColumns = "id",
                        childColumns = "productId")},
        indices = {@Index("locationId"), @Index("productId")})
public class Batch {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** The product: its name, variant, package size, and unit. */
    public long productId;

    @NonNull
    public ItemCategory category = ItemCategory.FOOD;

    /** A whole number from 0 to 9999, counted in the product's unit. */
    public int quantity;

    /** The complete date printed on the package, or null when there is no complete date. */
    @Nullable
    public LocalDate date;

    /**
     * When only part of a date is printed or readable: what the label shows, exactly as the
     * household typed it (for example "03/2027"). Null when there is a complete date or none.
     */
    @Nullable
    public String printedDateText;

    @NonNull
    public DateLabel dateLabel = DateLabel.BEST_BY;

    /** True after the household confirmed the complete date and its label type. */
    public boolean dateConfirmed;

    public long locationId;

    /** Food batches only: the kind of package, or null for "Not chosen". */
    @Nullable
    public FoodType foodType;

    // Food batches only. Each answer is YES, NO, or null for "Not checked".
    @Nullable
    public ConditionAnswer sealed;

    @Nullable
    public ConditionAnswer shelfStable;

    @Nullable
    public ConditionAnswer damaged;

    @Nullable
    public ConditionAnswer storageConcern;

    /** True for an archived batch: Inventory hides it, and the rules never see it. */
    public boolean archived;

    /** True for made-up sample data. The screens mark it "Sample item". */
    public boolean sample;

    /** A copy with every value except the id, for the part of a batch that moves. */
    @NonNull
    public Batch copyWithoutId() {
        Batch copy = new Batch();
        copy.productId = productId;
        copy.category = category;
        copy.quantity = quantity;
        copy.date = date;
        copy.printedDateText = printedDateText;
        copy.dateLabel = dateLabel;
        copy.dateConfirmed = dateConfirmed;
        copy.locationId = locationId;
        copy.foodType = foodType;
        copy.sealed = sealed;
        copy.shelfStable = shelfStable;
        copy.damaged = damaged;
        copy.storageConcern = storageConcern;
        copy.archived = archived;
        copy.sample = sample;
        return copy;
    }
}

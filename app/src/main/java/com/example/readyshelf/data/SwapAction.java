package com.example.readyshelf.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * One row of the "swap_actions" table: the receipt of one swap that the household confirmed.
 *
 * <p>The Review swap screen makes one action ID and uses it for every tap on "Confirm swap".
 * The swap is saved together with this row, so a second tap with the same action ID finds the
 * row and changes nothing. Unconfirmed proposals are never stored, so no stock is held back.
 *
 * <p>CASCADE: when a batch is deleted (only "Remove sample data" deletes batches), its swap
 * receipts are deleted with it.
 */
@Entity(tableName = "swap_actions",
        foreignKeys = {
                @ForeignKey(entity = Batch.class,
                        parentColumns = "id",
                        childColumns = "kitBatchId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Batch.class,
                        parentColumns = "id",
                        childColumns = "pantryBatchId",
                        onDelete = ForeignKey.CASCADE)},
        indices = {@Index("kitBatchId"), @Index("pantryBatchId")})
public class SwapAction {

    /** A random text made once by the Review swap screen, for example a UUID. */
    @PrimaryKey
    @NonNull
    public String actionId = "";

    /** The kit batch whose units moved to the pantry (its id before any split). */
    public long kitBatchId;

    /** The pantry batch whose units moved into the kit (its id before any split). */
    public long pantryBatchId;

    /** How many units moved each way. */
    public int quantity;

    /** The phone's date when the swap was rechecked and saved. */
    @NonNull
    public LocalDate planDate = LocalDate.of(1970, 1, 1);

    /** When it was saved, in milliseconds since January 1, 1970 (System.currentTimeMillis()). */
    public long appliedAtMillis;
}

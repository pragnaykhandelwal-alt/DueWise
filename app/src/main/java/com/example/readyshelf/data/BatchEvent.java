package com.example.readyshelf.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One row of the "batch_events" table: one entry in a batch's history.
 *
 * <p>CASCADE means that when a batch is deleted, its history is deleted with it.
 */
@Entity(tableName = "batch_events",
        foreignKeys = @ForeignKey(entity = Batch.class,
                parentColumns = "id",
                childColumns = "batchId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("batchId"))
public class BatchEvent {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long batchId;

    /** When it happened, in milliseconds since January 1, 1970 (System.currentTimeMillis()). */
    public long occurredAtMillis;

    @NonNull
    public BatchEventType type = BatchEventType.EDITED;

    /** A sentence for people, for example "Quantity changed from 6 to 4." */
    @NonNull
    public String details = "";

    /** A new history entry for one batch, ready to insert. */
    @NonNull
    public static BatchEvent of(long batchId, @NonNull BatchEventType type, @NonNull String details,
                                long occurredAtMillis) {
        BatchEvent event = new BatchEvent();
        event.batchId = batchId;
        event.occurredAtMillis = occurredAtMillis;
        event.type = type;
        event.details = details;
        return event;
    }
}

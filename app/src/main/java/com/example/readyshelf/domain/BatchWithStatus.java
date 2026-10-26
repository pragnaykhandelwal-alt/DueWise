package com.example.readyshelf.domain;

import androidx.annotation.NonNull;

import java.util.Objects;

/** A batch together with the status that the date rules gave it. Never changes. */
public final class BatchWithStatus {

    @NonNull private final BatchInfo batch;
    @NonNull private final DateStatus status;

    public BatchWithStatus(@NonNull BatchInfo batch, @NonNull DateStatus status) {
        this.batch = Objects.requireNonNull(batch, "batch");
        this.status = Objects.requireNonNull(status, "status");
    }

    @NonNull
    public BatchInfo getBatch() {
        return batch;
    }

    @NonNull
    public DateStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BatchWithStatus)) {
            return false;
        }
        BatchWithStatus that = (BatchWithStatus) other;
        return batch.equals(that.batch) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batch, status);
    }

    @NonNull
    @Override
    public String toString() {
        return "BatchWithStatus{" + batch + ", status=" + status + "}";
    }
}

package com.example.readyshelf.data;

/**
 * What happened to a batch, for its history.
 *
 * <p>The database stores each constant's name, so renaming a constant later
 * needs a database migration. Adding a constant doesn't.
 */
public enum BatchEventType {
    ADDED,
    EDITED,
    MOVED,
    /** Recorded as used. For medicine, this is only a stock record. */
    USED,
    DISCARDED,
    ARCHIVED,
    /** Part of a confirmed swap between a kit batch and a pantry batch (added 2026-10-11). */
    SWAPPED
}

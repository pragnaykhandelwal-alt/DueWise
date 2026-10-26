package com.example.readyshelf.domain;

/**
 * The kind of place where batches are kept. Every location has one type.
 *
 * <p>The database stores each constant's name (for example "PANTRY"),
 * so renaming a constant later needs a database migration.
 */
public enum LocationType {
    PANTRY,
    EMERGENCY_KIT,
    FIRST_AID,
    OTHER
}

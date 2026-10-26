package com.example.readyshelf.domain;

/**
 * The kind of package a food batch comes in. The household chooses it on the batch form;
 * a new batch starts with no food type (null, shown as "Not chosen").
 *
 * <p>The database stores each constant's name, so renaming a constant later
 * needs a database migration.
 */
public enum FoodType {
    CANNED,
    DRY_OR_BOXED,
    JAR_OR_BOTTLE,
    POUCH,
    FROZEN,
    REFRIGERATED,
    OTHER
}

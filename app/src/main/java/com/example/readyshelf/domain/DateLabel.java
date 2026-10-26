package com.example.readyshelf.domain;

/**
 * The kind of date printed on the package, as the household reads it from the label.
 *
 * <p>The database stores each constant's name, so renaming a constant later
 * needs a database migration.
 */
public enum DateLabel {
    BEST_BY,
    USE_BY,
    EXPIRES,
    OTHER
}

package com.example.readyshelf.domain;

/**
 * What kind of food a product is, as the household classified it. A missing value (null) means
 * "unclassified".
 *
 * <p>Stored with each product since database version 3. Until 2026-10-22 the household chose it
 * on the batch form, and the swap rules used it. Since then (the student's choice) nothing asks,
 * shows, or uses it, but every product keeps the class it has, so the database doesn't change.
 */
public enum FoodClass {
    SHELF_STABLE_FOOD,
    PERISHABLE_FOOD,
    INFANT_FORMULA
}

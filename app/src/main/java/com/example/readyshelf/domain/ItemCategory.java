package com.example.readyshelf.domain;

/**
 * What kind of item a batch holds.
 *
 * <p>The database stores each constant's name, so renaming a constant later
 * needs a database migration. A new constant needs none, but an older version of the app
 * can't read a batch saved with it. The screens list the categories in this order, so
 * OTHER stays last. Medicine, documents, and subscriptions are never swapped (since
 * 2026-10-22; until then only FOOD was swapped). A new constant stays out of swaps and
 * replacement needs until RotationPlanner and RuleRunner name it, and out of the Purchase/Renewal
 * list's Renewal section until RenewalList names it (documents and subscriptions since
 * 2026-10-24).
 */
public enum ItemCategory {
    FOOD,
    WATER,
    MEDICINE,
    FIRST_AID_SUPPLY,
    COSMETIC,
    DOCUMENT,
    SUBSCRIPTION,
    OTHER
}

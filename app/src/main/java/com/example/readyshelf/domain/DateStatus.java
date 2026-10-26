package com.example.readyshelf.domain;

/**
 * The date status of one batch. The student's DatePolicy decides which status a batch has
 * (the rules are listed in SPEC.md, "The student's rules").
 *
 * <p>What the app does with each status, once DatePolicy is connected:
 * <ul>
 *   <li>Inventory and Batch details show it as text.</li>
 *   <li>Home lists every status except LATER under "Needs review".</li>
 * </ul>
 * Statuses are never stored in the database.
 */
public enum DateStatus {
    /** Shown as "Date needs review". */
    NEEDS_REVIEW,
    /** Shown as "Date passed". */
    PAST_DATE,
    /** Shown as "Date is today". */
    TODAY,
    /** Shown as "Near date". */
    NEAR_DATE,
    /** Shown as "Later date". */
    LATER
}

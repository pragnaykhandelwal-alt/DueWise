package com.example.readyshelf.domain;

/**
 * The household's answer to one food condition question, such as "Sealed?".
 *
 * <p>A missing answer (null) means "Not checked". Every new batch starts with no answers,
 * so the form never answers a question for the household.
 *
 * <p>The database stores each constant's name, so renaming a constant later
 * needs a database migration.
 */
public enum ConditionAnswer {
    YES,
    NO
}

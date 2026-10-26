package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for ReminderList: which items the daily reminder names, how many more it counts, and
 * (since 2026-10-23) the number for the app icon, Home's tiles added up.
 *
 * <p>These tests don't test any date rules: each item gets its status here, the way the date
 * rules would give it, and the review lists are already in Home's order.
 */
public class ReminderListTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 19);

    // ----- How many entries are shown -----

    @Test
    public void nothingCausesAReminder_givesTheEmptyList() {
        ReminderList list = ReminderList.of(Arrays.asList(
                food(1, "Canned beans", DateStatus.LATER),
                food(2, "Granola bars", DateStatus.NEEDS_REVIEW)));

        assertTrue(list.isEmpty());
        assertEquals(0, list.getMoreCount());
        assertEquals(0, list.getIconNumber());
        assertEquals(0, ReminderList.EMPTY.getIconNumber());
        assertEquals(ReminderList.EMPTY, list);
        assertEquals(ReminderList.EMPTY, ReminderList.of(Collections.emptyList()));
    }

    @Test
    public void oneItem_isShownAlone() {
        ReminderList list = ReminderList.of(Collections.singletonList(
                food(1, "Canned beans", DateStatus.NEAR_DATE)));

        assertEquals(names("Canned beans"), list.getShown());
        assertEquals(0, list.getMoreCount());
        assertFalse(list.isEmpty());
    }

    @Test
    public void twoItems_areBothShown_withNothingMore() {
        ReminderList list = ReminderList.of(Arrays.asList(
                food(1, "Canned beans", DateStatus.PAST_DATE),
                food(2, "Granola bars", DateStatus.TODAY)));

        assertEquals(names("Canned beans", "Granola bars"), list.getShown());
        assertEquals(0, list.getMoreCount());
    }

    @Test
    public void fiveItems_showTheFirstTwo_andCountThreeMore() {
        // The list keeps the review list's order (soonest date first).
        ReminderList list = ReminderList.of(Arrays.asList(
                food(1, "Canned beans", DateStatus.PAST_DATE),
                food(2, "Granola bars", DateStatus.TODAY),
                food(3, "Rice", DateStatus.NEAR_DATE),
                food(4, "Pasta", DateStatus.NEAR_DATE),
                food(5, "Peanut butter", DateStatus.NEAR_DATE)));

        assertEquals(names("Canned beans", "Granola bars"), list.getShown());
        assertEquals(3, list.getMoreCount());
        assertEquals(5, list.getIconNumber());
    }

    // ----- Which items count -----

    @Test
    public void itemsThatCauseNoReminder_areNeitherShownNorCounted() {
        // A later date, a date that needs review, a sample item, and a batch with nothing left.
        ReminderList list = ReminderList.of(Arrays.asList(
                food(1, "Later soup", DateStatus.LATER),
                food(2, "Unchecked soup", DateStatus.NEEDS_REVIEW),
                new BatchWithStatus(batch(3, "Sample bars", ItemCategory.FOOD, 6, true),
                        DateStatus.PAST_DATE),
                new BatchWithStatus(batch(4, "Empty rice", ItemCategory.FOOD, 0, false),
                        DateStatus.TODAY),
                food(5, "Canned beans", DateStatus.NEAR_DATE)));

        assertEquals(names("Canned beans"), list.getShown());
        assertEquals(0, list.getMoreCount());
        // The icon number counts like Home's tiles: the sample item and the empty batch too.
        assertEquals(3, list.getIconNumber());
    }

    @Test
    public void aName_isListedOnce_whateverItsCapitalLettersAndSpaces() {
        // Three batches of beans, one of them typed differently. The first spelling is kept,
        // and "1 more" counts names (Rice), not batches.
        ReminderList list = ReminderList.of(Arrays.asList(
                food(1, "Canned beans", DateStatus.PAST_DATE),
                food(2, "canned beans ", DateStatus.NEAR_DATE),
                food(3, "Granola bars", DateStatus.NEAR_DATE),
                food(4, "Canned beans", DateStatus.NEAR_DATE),
                food(5, "Rice", DateStatus.NEAR_DATE)));

        assertEquals(names("Canned beans", "Granola bars"), list.getShown());
        assertEquals(1, list.getMoreCount());
        // The icon number counts batches, like Home's tiles.
        assertEquals(5, list.getIconNumber());
    }

    @Test
    public void spacesAroundAName_areNotShown() {
        ReminderList list = ReminderList.of(Collections.singletonList(
                food(1, "  Canned beans ", DateStatus.TODAY)));

        assertEquals(names("Canned beans"), list.getShown());
    }

    // ----- Medicine is never named -----

    @Test
    public void medicineIsNeverNamed_andAllMedicineIsOneEntry() {
        ReminderList list = ReminderList.of(Arrays.asList(
                medicine(1, "Pain reliever tablets", DateStatus.PAST_DATE),
                food(2, "Canned beans", DateStatus.TODAY),
                medicine(3, "Cough syrup", DateStatus.NEAR_DATE),
                food(4, "Granola bars", DateStatus.NEAR_DATE)));

        assertEquals(Arrays.asList(ReminderList.Entry.MEDICINE,
                ReminderList.Entry.named("Canned beans")), list.getShown());
        // Only the granola bars: the cough syrup is part of the medicine entry.
        assertEquals(1, list.getMoreCount());
        assertTrue(list.getShown().get(0).isMedicine());
        assertEquals("", list.getShown().get(0).getName());
        // Each medicine batch counts in the icon number, like on Home's tiles.
        assertEquals(4, list.getIconNumber());
    }

    @Test
    public void medicineAfterTheFirstTwo_isCountedAsOneEntry() {
        ReminderList list = ReminderList.of(Arrays.asList(
                food(1, "Canned beans", DateStatus.PAST_DATE),
                food(2, "Granola bars", DateStatus.TODAY),
                medicine(3, "Pain reliever tablets", DateStatus.NEAR_DATE),
                medicine(4, "Cough syrup", DateStatus.NEAR_DATE)));

        assertEquals(names("Canned beans", "Granola bars"), list.getShown());
        assertEquals(1, list.getMoreCount());
    }

    @Test
    public void otherCategories_areNamedLikeFood() {
        ReminderList list = ReminderList.of(Arrays.asList(
                new BatchWithStatus(batch(1, "Bandages", ItemCategory.FIRST_AID_SUPPLY, 1, false),
                        DateStatus.NEAR_DATE),
                new BatchWithStatus(batch(2, "Passport", ItemCategory.DOCUMENT, 1, false),
                        DateStatus.NEAR_DATE)));

        assertEquals(names("Bandages", "Passport"), list.getShown());
        assertFalse(list.getShown().get(0).isMedicine());
    }

    // ----- The number for the app icon (2026-10-23) -----

    @Test
    public void onlyAnOldSampleItem_givesNoEntries_butAnIconNumber() {
        // Nothing causes a reminder, so no reminder carries the number.
        ReminderList list = ReminderList.of(Collections.singletonList(
                new BatchWithStatus(batch(1, "Sample bars", ItemCategory.FOOD, 6, true),
                        DateStatus.PAST_DATE)));

        assertTrue(list.isEmpty());
        assertEquals(1, list.getIconNumber());
        assertNotEquals(ReminderList.EMPTY, list);
    }

    @Test
    public void theIconNumber_isPartOfEquality() {
        List<BatchWithStatus> beans = Collections.singletonList(
                food(1, "Canned beans", DateStatus.NEAR_DATE));
        List<BatchWithStatus> beansAndSample = Arrays.asList(
                food(1, "Canned beans", DateStatus.NEAR_DATE),
                new BatchWithStatus(batch(2, "Sample bars", ItemCategory.FOOD, 6, true),
                        DateStatus.PAST_DATE));

        // The same words, but a different number.
        assertEquals(ReminderList.of(beans).getShown(), ReminderList.of(beansAndSample).getShown());
        assertNotEquals(ReminderList.of(beans), ReminderList.of(beansAndSample));
        assertEquals(ReminderList.of(beans), ReminderList.of(beans));
        assertEquals(ReminderList.of(beans).hashCode(), ReminderList.of(beans).hashCode());
    }

    @Test
    public void theShownEntries_cantBeChanged() {
        ReminderList list = ReminderList.of(Collections.singletonList(
                food(1, "Canned beans", DateStatus.TODAY)));

        assertThrows(UnsupportedOperationException.class,
                () -> list.getShown().add(ReminderList.Entry.MEDICINE));
    }

    // ----- Test helpers -----

    private static BatchWithStatus food(long id, String name, DateStatus status) {
        return new BatchWithStatus(batch(id, name, ItemCategory.FOOD, 4, false), status);
    }

    private static BatchWithStatus medicine(long id, String name, DateStatus status) {
        return new BatchWithStatus(batch(id, name, ItemCategory.MEDICINE, 10, false), status);
    }

    private static BatchInfo batch(long id, String name, ItemCategory category, int quantity,
                                   boolean sample) {
        return new BatchInfo(id, name, category, quantity, "units", DAY.plusDays(id),
                DateLabel.BEST_BY, 1, "Pantry", LocationType.PANTRY, sample);
    }

    private static List<ReminderList.Entry> names(String... names) {
        List<ReminderList.Entry> entries = new ArrayList<>();
        for (String name : names) {
            entries.add(ReminderList.Entry.named(name));
        }
        return entries;
    }
}

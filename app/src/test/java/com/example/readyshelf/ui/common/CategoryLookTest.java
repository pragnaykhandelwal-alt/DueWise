package com.example.readyshelf.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.example.readyshelf.domain.ItemCategory;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Every category has a name and its own icon. A new category without them would stop the batch
 * cards, Inventory's chips, and the batch form with an error.
 */
public class CategoryLookTest {

    @Test
    public void everyCategory_hasANameAndAnIcon() {
        for (ItemCategory category : ItemCategory.values()) {
            assertNotEquals(category.name(), 0, DisplayNames.category(category));
            assertNotEquals(category.name(), 0, CategoryLook.icon(category));
        }
    }

    @Test
    public void noTwoCategories_shareANameOrAnIcon() {
        Set<Integer> names = new HashSet<>();
        Set<Integer> icons = new HashSet<>();
        for (ItemCategory category : ItemCategory.values()) {
            names.add(DisplayNames.category(category));
            icons.add(CategoryLook.icon(category));
        }
        assertEquals(ItemCategory.values().length, names.size());
        assertEquals(ItemCategory.values().length, icons.size());
    }

    @Test
    public void otherStaysLast_soTheScreensListItLast() {
        ItemCategory[] all = ItemCategory.values();
        assertEquals(ItemCategory.OTHER, all[all.length - 1]);
    }
}

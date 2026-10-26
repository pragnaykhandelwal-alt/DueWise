package com.example.readyshelf.ui.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.ItemCategory;

import org.junit.Test;

/** Tests for what the batch form starts with for each category. */
public class CategoryDefaultsTest {

    @Test
    public void startingLabel_isExpires_forItemsWhoseLabelsUsuallySayIt() {
        for (ItemCategory category : new ItemCategory[] {ItemCategory.MEDICINE,
                ItemCategory.FIRST_AID_SUPPLY, ItemCategory.COSMETIC, ItemCategory.DOCUMENT,
                ItemCategory.SUBSCRIPTION}) {
            assertEquals(category.name(), DateLabel.EXPIRES, CategoryDefaults.startingLabel(category));
        }
    }

    @Test
    public void startingLabel_staysBestBy_forFoodWaterAndOther() {
        // Food keeps the label type the form always started with.
        assertEquals(DateLabel.BEST_BY, CategoryDefaults.startingLabel(ItemCategory.FOOD));
        assertEquals(DateLabel.BEST_BY, CategoryDefaults.startingLabel(ItemCategory.WATER));
        assertEquals(DateLabel.BEST_BY, CategoryDefaults.startingLabel(ItemCategory.OTHER));
    }

    @Test
    public void onlyDocumentsAndSubscriptions_areOneItem() {
        for (ItemCategory category : ItemCategory.values()) {
            boolean expected = category == ItemCategory.DOCUMENT
                    || category == ItemCategory.SUBSCRIPTION;
            assertEquals(category.name(), expected, CategoryDefaults.isOneItem(category));
        }
    }

    @Test
    public void aOneItemCategory_hasItsOwnUnit_andTheOthersHaveNone() {
        assertNotEquals(0, CategoryDefaults.oneItemUnit(ItemCategory.DOCUMENT));
        assertNotEquals(0, CategoryDefaults.oneItemUnit(ItemCategory.SUBSCRIPTION));
        assertNotEquals(CategoryDefaults.oneItemUnit(ItemCategory.DOCUMENT),
                CategoryDefaults.oneItemUnit(ItemCategory.SUBSCRIPTION));
        assertEquals(0, CategoryDefaults.oneItemUnit(ItemCategory.FOOD));
        assertEquals(0, CategoryDefaults.oneItemUnit(ItemCategory.COSMETIC));
    }

    @Test
    public void everyCategory_hasItsOwnUnitChoices() {
        for (ItemCategory category : ItemCategory.values()) {
            assertNotEquals(category.name(), 0, CategoryDefaults.unitSuggestions(category));
        }
        assertNotEquals(CategoryDefaults.unitSuggestions(ItemCategory.FOOD),
                CategoryDefaults.unitSuggestions(ItemCategory.COSMETIC));
    }
}

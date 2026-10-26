package com.example.readyshelf.ui.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.SavedStateHandle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.ConditionAnswer;
import com.example.readyshelf.domain.DateEntry;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.FoodType;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.ui.photo.PhotoEntryResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * The streamlined batch form (2026-10-19): the label type that follows the category, a new
 * document or subscription counted as one item, "Save and add another", and when Back asks
 * "Leave without saving?". The simpler form (2026-10-22): the date's folded parts ("Change" for
 * the label type, and "What is printed?"), and since later that day no food section. Like
 * BatchEditPhotoResultTest, it never saves: the ViewModel is made on the main thread and only
 * its choices and review are read.
 */
@RunWith(AndroidJUnit4.class)
public class BatchEditFormTest {

    private static final long NO_SUCH_ID = 987654; // nothing is loaded or saved for it

    @NonNull
    private static Application application() {
        return (Application) InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getApplicationContext();
    }

    private static void onMain(@NonNull Runnable step) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(step);
    }

    /** Makes the form's ViewModel on the main thread and starts it with {@code start}. */
    @NonNull
    private static BatchEditViewModel form(@NonNull Starter start) {
        BatchEditViewModel[] made = new BatchEditViewModel[1];
        onMain(() -> {
            made[0] = new BatchEditViewModel(application(), new SavedStateHandle());
            start.start(made[0]);
        });
        return made[0];
    }

    private interface Starter {
        void start(@NonNull BatchEditViewModel form);
    }

    @NonNull
    private static BatchEditViewModel newBatchForm() {
        return form(form -> form.start(0));
    }

    @Test
    public void theLabelType_followsTheCategory_untilTheHouseholdChoosesOne() {
        BatchEditViewModel form = newBatchForm();
        assertEquals(DateLabel.BEST_BY, form.getDateLabel()); // Food, as before

        onMain(() -> form.setCategory(ItemCategory.DOCUMENT));
        assertEquals(DateLabel.EXPIRES, form.getDateLabel());

        onMain(() -> {
            form.setDateLabel(DateLabel.OTHER);
            form.setCategory(ItemCategory.FOOD);
        });
        assertEquals(DateLabel.OTHER, form.getDateLabel());
    }

    @Test
    public void aLabelTypeThatFollowsTheCategory_removesTheConfirmation() {
        BatchEditViewModel form = newBatchForm();
        onMain(() -> {
            form.setDate(LocalDate.of(2027, 5, 1));
            form.setDateConfirmed(true);
            form.setCategory(ItemCategory.WATER); // Best by, like Food: nothing changes
        });
        assertTrue(form.isDateConfirmed());

        onMain(() -> form.setCategory(ItemCategory.COSMETIC)); // now Expires
        assertEquals(DateLabel.EXPIRES, form.getDateLabel());
        assertFalse(form.isDateConfirmed());
    }

    @Test
    public void aMedicineClue_alsoMovesALabelTypeThatWasNotChosen() {
        BatchEditViewModel form = newBatchForm();
        onMain(() -> form.applyPhotoResult(PhotoEntryResult.of(null, Collections.emptyList(),
                Collections.singletonList("Drug Facts"))));

        assertEquals(ItemCategory.MEDICINE, form.getCategory());
        assertEquals(DateLabel.EXPIRES, form.getDateLabel());
    }

    @Test
    public void aNewDocumentOrSubscription_isOneItem() {
        BatchEditViewModel form = newBatchForm();
        assertFalse(form.isOneItem());

        onMain(() -> form.setCategory(ItemCategory.DOCUMENT));
        assertTrue(form.isOneItem());
        assertEquals("document", form.oneItemUnit());

        onMain(() -> form.setCategory(ItemCategory.SUBSCRIPTION));
        assertEquals("subscription", form.oneItemUnit());

        onMain(() -> form.setCategory(ItemCategory.COSMETIC));
        assertFalse(form.isOneItem());
        assertEquals("", form.oneItemUnit());
    }

    @Test
    public void editingOrRecordingABoughtItem_alwaysAsksTheQuantity() {
        BatchEditViewModel editing = form(form -> form.start(NO_SUCH_ID));
        onMain(() -> editing.setCategory(ItemCategory.DOCUMENT));
        assertFalse(editing.isOneItem());

        BatchEditViewModel purchase = form(form -> form.startPurchase(NO_SUCH_ID, NO_SUCH_ID,
                NO_SUCH_ID, 3));
        onMain(() -> purchase.setCategory(ItemCategory.SUBSCRIPTION));
        assertFalse(purchase.isOneItem());
    }

    @Test
    public void theReview_showsADocumentAsOneDocument() {
        BatchEditViewModel form = newBatchForm();
        onMain(() -> {
            form.setCategory(ItemCategory.DOCUMENT);
            form.setNewProduct(true);
            form.setDate(LocalDate.of(2031, 3, 14));
            form.startReview(new BatchEditViewModel.TypedInput("Test passport",
                    form.oneItemUnit(), 1));
        });

        BatchEditViewModel.State state = form.getState().getValue();
        assertNotNull(state);
        assertTrue(state.reviewText, state.reviewText.contains("Category: Documents"));
        assertTrue(state.reviewText, state.reviewText.contains("Quantity: 1 document"));
        assertTrue(state.reviewText, state.reviewText.contains("Expires: 14 Mar 2031"));
    }

    @Test
    public void anotherBatch_startsWithTheCategoryAndLocationJustUsed() {
        BatchEditViewModel form = form(f -> f.startAnother(ItemCategory.COSMETIC, NO_SUCH_ID));

        assertEquals(ItemCategory.COSMETIC, form.getCategory());
        assertEquals(NO_SUCH_ID, form.getLocationId());
        assertEquals(DateLabel.EXPIRES, form.getDateLabel());
        assertFalse(form.isEditing());
        // A new form: the date's folded parts are closed, and nothing is ticked.
        assertFalse(form.isDateLabelOpen());
        assertFalse(form.isDateKindOpen());
        assertFalse(form.isDateConfirmed());

        // The category counts as the household's own choice, so a food clue doesn't replace it.
        onMain(() -> form.applyPhotoResult(PhotoEntryResult.of(null,
                Collections.singletonList("Nutrition Facts"), Collections.emptyList())));
        assertEquals(ItemCategory.COSMETIC, form.getCategory());
    }

    @Test
    public void saveAndAddAnother_isOfferedOnlyWhenAdding() {
        assertTrue(newBatchForm().canAddAnother());
        assertFalse(form(form -> form.start(NO_SUCH_ID)).canAddAnother());
        assertFalse(form(form -> form.startPurchase(NO_SUCH_ID, NO_SUCH_ID, NO_SUCH_ID, 3))
                .canAddAnother());
    }

    // ----- "Leave without saving?" (2026-10-19) -----

    /** The typed fields in the order the screen gives them: name, unit, quantity. */
    @NonNull
    private static ArrayList<String> typed(@NonNull String name, @NonNull String quantity) {
        return new ArrayList<>(Arrays.asList(name, "", quantity));
    }

    @Test
    public void everyKindOfForm_startsWithNothingToLose() {
        BatchEditViewModel added = newBatchForm();
        BatchEditViewModel another = form(f -> f.startAnother(ItemCategory.WATER, NO_SUCH_ID));
        BatchEditViewModel bought = form(f -> f.startPurchase(NO_SUCH_ID, NO_SUCH_ID,
                NO_SUCH_ID, 3));
        for (BatchEditViewModel form : Arrays.asList(added, another, bought)) {
            assertFalse(form.isTouched());
            // The screen hasn't said yet how it filled the fields: nothing can be lost.
            assertFalse(form.hasUnsavedInput(typed("", "1")));
        }
        // "Record bought item" fills in its product and quantity; that is not the household's.
        onMain(() -> bought.setTextBaseline(typed("", "3")));
        assertFalse(bought.hasUnsavedInput(typed("", "3")));
    }

    @Test
    public void typingAField_isSomethingToLose_untilItIsAsFilledAgain() {
        BatchEditViewModel form = newBatchForm();
        onMain(() -> form.setTextBaseline(typed("", "1")));

        assertTrue(form.hasUnsavedInput(typed("Rice", "1")));
        assertTrue(form.hasUnsavedInput(typed("", "4")));
        assertTrue(form.hasUnsavedInput(new ArrayList<>(Arrays.asList("", "cans", "1")))); // a unit
        assertFalse(form.hasUnsavedInput(typed("", "1")));
    }

    @Test
    public void aChangeByHand_isSomethingToLose_andItIsKept() {
        SavedStateHandle saved = new SavedStateHandle();
        BatchEditViewModel[] made = new BatchEditViewModel[2];
        onMain(() -> {
            made[0] = new BatchEditViewModel(application(), saved);
            made[0].start(0);
            made[0].setTextBaseline(typed("", "1"));
            made[0].markTouched();
        });
        assertTrue(made[0].hasUnsavedInput(typed("", "1")));

        // A form made again from the saved state (Android closed and reopened the app) still
        // knows it, so Back still asks.
        onMain(() -> {
            made[1] = new BatchEditViewModel(application(), saved);
            made[1].start(0);
        });
        assertTrue(made[1].isTouched());
        assertEquals(typed("", "1"), made[1].getTextBaseline());
    }

    // ----- No food section (2026-10-22, later that day) -----

    /** A Food batch of the household's, as Batch details would load it for an edit. */
    @NonNull
    private static BatchInfo foodBatch(@Nullable LocalDate date, @Nullable String printed,
                                       @Nullable FoodType foodType,
                                       @Nullable ConditionAnswer sealed,
                                       @Nullable ConditionAnswer shelfStable,
                                       @Nullable ConditionAnswer damaged,
                                       @Nullable ConditionAnswer storageConcern) {
        return new BatchInfo(NO_SUCH_ID, NO_SUCH_ID, "Test soup", "", "", "cans", null,
                ItemCategory.FOOD, 2, date, printed, DateLabel.BEST_BY, date != null,
                NO_SUCH_ID, "Pantry", LocationType.PANTRY, foodType, sealed, shelfStable,
                damaged, storageConcern, false, false);
    }

    /** The edit form with {@code batch} loaded into it, as when Batch details opens it. */
    @NonNull
    private static BatchEditViewModel editForm(@NonNull BatchInfo batch) {
        BatchEditViewModel form = form(f -> f.start(NO_SUCH_ID));
        onMain(() -> form.takeLoadedBatch(batch));
        return form;
    }

    @NonNull
    private static String reviewText(@NonNull BatchEditViewModel form) {
        BatchEditViewModel.State state = form.getState().getValue();
        assertNotNull(state);
        return state.reviewText;
    }

    @Test
    public void theReviewOfANewFoodBatch_saysNothingAboutFood() {
        BatchEditViewModel form = newBatchForm();
        onMain(() -> {
            form.setNewProduct(true);
            form.setDate(LocalDate.of(2027, 5, 1));
            form.startReview(new BatchEditViewModel.TypedInput("Test soup", "cans", 2));
        });
        String review = reviewText(form);
        assertTrue(review, review.contains("Category: Food"));
        assertFalse(review, review.contains("Food condition"));
        assertFalse(review, review.contains("Food class"));
        assertFalse(review, review.contains("Sealed"));
    }

    @Test
    public void anEditOfAnOlderFoodBatch_namesOnlyWhatTheFormChanged() {
        // The batch has a food type and answers from before 2026-10-22. The form no longer shows
        // them, and an edit doesn't change them (ReadyShelfRepositoryTest checks that the stored
        // values stay).
        BatchEditViewModel form = editForm(foodBatch(LocalDate.of(2026, 12, 15), null,
                FoodType.CANNED, ConditionAnswer.NO, ConditionAnswer.YES, null, null));
        // "New product" makes a review without the database's products (it adds its own line).
        onMain(() -> {
            form.setNewProduct(true);
            form.startReview(new BatchEditViewModel.TypedInput("Test soup", "cans", 3));
        });
        String review = reviewText(form);
        assertTrue(review, review.contains("Count corrected from 2 cans to 3 cans."));
        for (String word : new String[] {"Food type", "Sealed", "Shelf-stable", "Damaged",
                "Storage concern", "removed"}) {
            assertFalse(word + " in: " + review, review.contains(word));
        }

        // A change to another category doesn't say that food answers were removed.
        onMain(() -> {
            form.cancelReview();
            form.setCategory(ItemCategory.WATER);
            form.startReview(new BatchEditViewModel.TypedInput("Test soup", "cans", 2));
        });
        review = reviewText(form);
        assertTrue(review, review.contains("Category changed from Food to Water."));
        assertFalse(review, review.contains("removed"));
    }

    // ----- The date's folded parts (2026-10-22) -----

    @Test
    public void theDateFolds_startClosed_andOpeningThemIsNotInput() {
        BatchEditViewModel added = newBatchForm();
        BatchEditViewModel another = form(f -> f.startAnother(ItemCategory.WATER, NO_SUCH_ID));
        BatchEditViewModel bought = form(f -> f.startPurchase(NO_SUCH_ID, NO_SUCH_ID,
                NO_SUCH_ID, 3));
        for (BatchEditViewModel form : Arrays.asList(added, another, bought)) {
            assertFalse(form.isDateLabelOpen());
            assertFalse(form.isDateKindOpen());
            onMain(() -> {
                form.setTextBaseline(typed("", "1"));
                form.openDateLabel();
                form.openDateKind();
            });
            assertTrue(form.isDateLabelOpen());
            assertTrue(form.isDateKindOpen());
            // Opening a part changes no answer, so Back doesn't ask.
            assertFalse(form.isTouched());
            assertFalse(form.hasUnsavedInput(typed("", "1")));
        }
        // "Change" isn't a choice: the label type still follows the category.
        onMain(() -> added.setCategory(ItemCategory.MEDICINE));
        assertEquals(DateLabel.EXPIRES, added.getDateLabel());
    }

    @Test
    public void whatIsPrinted_opensByItself_forAnEditWithoutAFullDate() {
        BatchEditViewModel partOfADate = editForm(foodBatch(null, "07/2027", null, null, null,
                null, null));
        assertEquals(DateEntry.Kind.INCOMPLETE, partOfADate.getDateKind());
        assertTrue(partOfADate.isDateKindOpen());

        BatchEditViewModel noDate = editForm(foodBatch(null, null, null, null, null, null, null));
        assertEquals(DateEntry.Kind.NONE, noDate.getDateKind());
        assertTrue(noDate.isDateKindOpen());

        BatchEditViewModel fullDate = editForm(foodBatch(LocalDate.of(2026, 12, 15), null, null,
                null, null, null, null));
        assertFalse(fullDate.isDateKindOpen());
        assertFalse(fullDate.isDateLabelOpen());
    }

    @Test
    public void choosingPartOfADate_keepsWhatIsPrintedOpen_alsoAfterAFullDateAgain() {
        BatchEditViewModel form = newBatchForm();
        onMain(() -> form.setDateKind(DateEntry.Kind.INCOMPLETE));
        assertTrue(form.isDateKindOpen());

        onMain(() -> form.setDateKind(DateEntry.Kind.COMPLETE));
        assertTrue(form.isDateKindOpen()); // the chosen answer stays in view
    }

    @Test
    public void theFolds_areKept_whenTheFormIsMadeAgain() {
        SavedStateHandle saved = new SavedStateHandle();
        onMain(() -> {
            BatchEditViewModel first = new BatchEditViewModel(application(), saved);
            first.start(0);
            first.openDateLabel();
            first.openDateKind();
        });

        // Made again from the saved state (Android closed and reopened the app).
        BatchEditViewModel[] again = new BatchEditViewModel[1];
        onMain(() -> {
            again[0] = new BatchEditViewModel(application(), saved);
            again[0].start(0);
        });
        assertTrue(again[0].isDateLabelOpen());
        assertTrue(again[0].isDateKindOpen());
    }
}

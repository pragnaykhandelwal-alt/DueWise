package com.example.readyshelf.ui.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.SavedStateHandle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.domain.DateCandidate;
import com.example.readyshelf.domain.DateEntry;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.PhotoDateChoices;
import com.example.readyshelf.ui.photo.PhotoEntryResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * How the batch form puts in what the household chose on the Label photos screen. It never
 * saves: the form's ViewModel is made on the main thread and only its choices are read.
 *
 * <p>The dates come from preset candidates (a fake parser that returns fixed answers, like
 * FixedStatusPolicy in the reminder tests). They are not a date-candidate parser.
 */
@RunWith(AndroidJUnit4.class)
public class BatchEditPhotoResultTest {

    private static final List<String> NO_LINES = Collections.emptyList();

    /** The choice the household would tap, made from one preset candidate. */
    @NonNull
    private static PhotoDateChoices.Choice choice(@NonNull DateCandidate candidate,
                                                  @NonNull PhotoDateChoices.Choice.Reading reading) {
        for (PhotoDateChoices.Choice choice : PhotoDateChoices.find(
                (lines, today) -> Collections.singletonList(candidate),
                Collections.singletonList("text"), LocalDate.of(2026, 10, 12)).getChoices()) {
            if (choice.getReading() == reading) {
                return choice;
            }
        }
        throw new AssertionError("No " + reading + " choice");
    }

    private static final DateCandidate BEST_BY_25_OCT = DateCandidate.complete("BEST BY 10/25/2026",
            "10/25/2026", DateLabel.BEST_BY, LocalDate.of(2026, 10, 25));
    private static final DateCandidate EXPIRES_MARCH = DateCandidate.monthAndYear("EXP 03/2027",
            "03/2027", DateLabel.EXPIRES, YearMonth.of(2027, 3));
    private static final DateCandidate TWO_READINGS = DateCandidate.twoReadings("USE BY 03/04/27",
            "03/04/27", DateLabel.USE_BY, LocalDate.of(2027, 3, 4), LocalDate.of(2027, 4, 3));

    @NonNull
    private static PhotoEntryResult result(@Nullable PhotoDateChoices.Choice date) {
        return PhotoEntryResult.of(date, NO_LINES, NO_LINES);
    }

    /** Makes the form's ViewModel for adding a batch, on the main thread. */
    @NonNull
    private static BatchEditViewModel newForm() {
        Application app = application();
        BatchEditViewModel[] made = new BatchEditViewModel[1];
        onMain(() -> {
            made[0] = new BatchEditViewModel(app, new SavedStateHandle());
            made[0].start(0);
        });
        return made[0];
    }

    @NonNull
    private static Application application() {
        return (Application) InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getApplicationContext();
    }

    private static void onMain(@NonNull Runnable step) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(step);
    }

    @NonNull
    private static BatchEditViewModel.State state(@NonNull BatchEditViewModel form) {
        BatchEditViewModel.State state = form.getState().getValue();
        assertNotNull(state);
        return state;
    }

    @Test
    public void aCompleteDate_isPutIn_unconfirmed_withItsLabel() {
        BatchEditViewModel form = newForm();
        onMain(() -> {
            form.setDate(LocalDate.of(2026, 1, 1));
            form.setDateConfirmed(true);
            form.applyPhotoResult(result(choice(BEST_BY_25_OCT, PhotoDateChoices.Choice.Reading.ONLY_READING)));
        });

        assertEquals(DateEntry.Kind.COMPLETE, form.getDateKind());
        assertEquals(LocalDate.of(2026, 10, 25), form.getDate());
        assertEquals(DateLabel.BEST_BY, form.getDateLabel());
        assertFalse(form.isDateConfirmed());
        assertTrue(state(form).dateFromPhoto);
    }

    @Test
    public void aMonthAndYear_isAnIncompleteDate_withNoDayMadeUp() {
        BatchEditViewModel form = newForm();
        onMain(() -> form.applyPhotoResult(result(
                choice(EXPIRES_MARCH, PhotoDateChoices.Choice.Reading.MONTH_AND_YEAR))));

        assertEquals(DateEntry.Kind.INCOMPLETE, form.getDateKind());
        assertEquals("03/2027", form.getPrintedDateText());
        assertNull(form.getDate());
        assertTrue(state(form).dateMonthOnly);
        // The household's own "Use the last day of the month" button is offered, not used.
        assertEquals(LocalDate.of(2027, 3, 31), form.getMonthEndChoice());
    }

    @Test
    public void keepAsPrinted_isAnIncompleteDate_withNoReadingPicked() {
        BatchEditViewModel form = newForm();
        onMain(() -> form.applyPhotoResult(result(
                choice(TWO_READINGS, PhotoDateChoices.Choice.Reading.AS_PRINTED))));

        assertEquals(DateEntry.Kind.INCOMPLETE, form.getDateKind());
        assertEquals("03/04/27", form.getPrintedDateText());
        assertTrue(state(form).dateAsPrinted);
        assertNull(form.getMonthEndChoice());
    }

    @Test
    public void theDayFirstReading_isTheDateTheHouseholdTapped() {
        BatchEditViewModel form = newForm();
        onMain(() -> form.applyPhotoResult(result(
                choice(TWO_READINGS, PhotoDateChoices.Choice.Reading.DAY_FIRST))));

        assertEquals(LocalDate.of(2027, 4, 3), form.getDate());
        assertEquals(DateLabel.USE_BY, form.getDateLabel());
    }

    // ----- "What is printed?" opens by itself (2026-10-22) -----

    @Test
    public void aPartOfADate_opensWhatIsPrinted_andItStaysOpen() {
        BatchEditViewModel form = newForm();
        assertFalse(form.isDateKindOpen());
        onMain(() -> form.applyPhotoResult(result(
                choice(EXPIRES_MARCH, PhotoDateChoices.Choice.Reading.MONTH_AND_YEAR))));
        assertTrue(form.isDateKindOpen());

        // "Use the last day of the month" makes it a full date; the answers stay in view.
        onMain(form::useMonthEnd);
        assertEquals(DateEntry.Kind.COMPLETE, form.getDateKind());
        assertTrue(form.isDateKindOpen());
    }

    @Test
    public void aFullDateFromAPhoto_leavesTheFoldsClosed() {
        BatchEditViewModel form = newForm();
        onMain(() -> form.applyPhotoResult(result(
                choice(BEST_BY_25_OCT, PhotoDateChoices.Choice.Reading.ONLY_READING))));

        assertEquals(DateEntry.Kind.COMPLETE, form.getDateKind());
        assertFalse(form.isDateKindOpen());
        assertFalse(form.isDateLabelOpen()); // the label line shows the photo's "Best by"
    }

    @Test
    public void anEarlierMonthEndChoice_isCleared() {
        BatchEditViewModel form = newForm();
        onMain(() -> {
            form.setDateKind(DateEntry.Kind.INCOMPLETE);
            form.setPrintedDateText("03/2027");
            form.useMonthEnd();
        });
        assertEquals("03/2027", state(form).monthEndFrom);

        onMain(() -> form.applyPhotoResult(result(
                choice(BEST_BY_25_OCT, PhotoDateChoices.Choice.Reading.ONLY_READING))));

        assertNull(state(form).monthEndFrom);
    }

    @Test
    public void noLabelWords_keepTheFormsLabel() {
        DateCandidate unlabeled = DateCandidate.complete("10/25/2026", "10/25/2026", null,
                LocalDate.of(2026, 10, 25));
        BatchEditViewModel form = newForm();
        onMain(() -> {
            form.setDateLabel(DateLabel.USE_BY);
            form.applyPhotoResult(result(choice(unlabeled, PhotoDateChoices.Choice.Reading.ONLY_READING)));
        });

        assertEquals(DateLabel.USE_BY, form.getDateLabel());
    }

    @Test
    public void withoutAChosenDate_theDateIsNotTouched() {
        BatchEditViewModel form = newForm();
        onMain(() -> {
            form.setDate(LocalDate.of(2026, 12, 1));
            form.setDateConfirmed(true);
            form.applyPhotoResult(result(null));
        });

        assertEquals(LocalDate.of(2026, 12, 1), form.getDate());
        assertTrue(form.isDateConfirmed());
        assertFalse(state(form).dateFromPhoto);
    }

    @Test
    public void changingTheDateByHand_endsThePhotoNote() {
        BatchEditViewModel form = newForm();
        onMain(() -> {
            form.applyPhotoResult(result(choice(BEST_BY_25_OCT, PhotoDateChoices.Choice.Reading.ONLY_READING)));
            form.setDate(LocalDate.of(2026, 11, 2));
        });

        assertFalse(state(form).dateFromPhoto);
    }

    @Test
    public void theReview_saysWhereTheDateCameFrom() {
        BatchEditViewModel form = newForm();
        onMain(() -> {
            form.applyPhotoResult(result(choice(BEST_BY_25_OCT, PhotoDateChoices.Choice.Reading.ONLY_READING)));
            form.setNewProduct(true);
            form.startReview(new BatchEditViewModel.TypedInput("Test bars", "bars", 2));
        });

        assertTrue(state(form).reviewText,
                state(form).reviewText.contains("Date read from a label photo: “BEST BY 10/25/2026”."));
    }

    @Test
    public void aMedicineClue_setsMedicine_butOtherCluesKeepAChoiceByHand() {
        BatchEditViewModel medicine = newForm();
        onMain(() -> medicine.applyPhotoResult(PhotoEntryResult.of(null, NO_LINES,
                Arrays.asList("Drug Facts"))));
        assertEquals(ItemCategory.MEDICINE, medicine.getCategory());
        assertTrue(state(medicine).photoCategoryApplied);

        BatchEditViewModel water = newForm();
        onMain(() -> {
            water.setCategory(ItemCategory.WATER);
            water.applyPhotoResult(PhotoEntryResult.of(null, Arrays.asList("Nutrition Facts"), NO_LINES));
        });
        assertEquals(ItemCategory.WATER, water.getCategory());
        assertFalse(state(water).photoCategoryApplied);
    }

    @Test
    public void nothingIsPutIn_whenEditing() {
        Application app = application();
        BatchEditViewModel[] made = new BatchEditViewModel[1];
        onMain(() -> {
            made[0] = new BatchEditViewModel(app, new SavedStateHandle());
            made[0].start(987654); // editing (a batch id that doesn't exist; nothing is loaded or saved)
            made[0].applyPhotoResult(result(choice(BEST_BY_25_OCT, PhotoDateChoices.Choice.Reading.ONLY_READING)));
        });

        assertNull(made[0].getDate());
        assertNull(state(made[0]).photoResult);
    }

    @Test
    public void nothingIsPutIn_whenRecordingABoughtItem() {
        Application app = application();
        BatchEditViewModel[] made = new BatchEditViewModel[1];
        onMain(() -> {
            made[0] = new BatchEditViewModel(app, new SavedStateHandle());
            made[0].startPurchase(987654, 987654, 987654, 3);
            made[0].applyPhotoResult(result(choice(BEST_BY_25_OCT, PhotoDateChoices.Choice.Reading.ONLY_READING)));
        });

        assertNull(made[0].getDate());
        assertNull(state(made[0]).photoResult);
    }
}

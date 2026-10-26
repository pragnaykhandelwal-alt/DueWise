package com.example.readyshelf.reminders;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.BatchWithStatus;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.ReminderList;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * The daily reminder's text (ReminderNotifier.listText) with the app's real words, for lists of
 * different lengths. Nothing is posted. The batches are made up.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReminderTextTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 19);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void oneName_isTheWholeText() {
        assertEquals("Canned beans", text(food(1, "Canned beans")));
    }

    @Test
    public void twoNames_areJoinedWithAnd() {
        assertEquals("Canned beans and Granola bars",
                text(food(1, "Canned beans"), food(2, "Granola bars")));
    }

    @Test
    public void moreThanTwoNames_showTheFirstTwo_andCountTheRest() {
        assertEquals("Canned beans, Granola bars, and 1 other",
                text(food(1, "Canned beans"), food(2, "Granola bars"), food(3, "Rice")));
        assertEquals("Canned beans, Granola bars, and 3 others",
                text(food(1, "Canned beans"), food(2, "Granola bars"), food(3, "Rice"),
                        food(4, "Pasta"), food(5, "Peanut butter")));
    }

    @Test
    public void medicine_isCalledMedicine_neverByItsName() {
        assertEquals("Medicine", text(medicine(1, "Pain reliever tablets")));
        assertEquals("Medicine and Canned beans",
                text(medicine(1, "Pain reliever tablets"), food(2, "Canned beans")));
        assertEquals("Canned beans, medicine, and 1 other",
                text(food(1, "Canned beans"), medicine(2, "Pain reliever tablets"),
                        medicine(3, "Cough syrup"), food(4, "Rice")));
    }

    @Test
    public void anEmptyList_givesTheGenericText() {
        assertEquals(context.getString(R.string.reminder_daily_text),
                ReminderNotifier.listText(context, ReminderList.EMPTY));
    }

    // ----- Helpers -----

    @NonNull
    private String text(@NonNull BatchWithStatus... items) {
        return ReminderNotifier.listText(context, ReminderList.of(Arrays.asList(items)));
    }

    /** A food batch with a near date. Each batch's date follows its id (soonest first). */
    @NonNull
    private static BatchWithStatus food(long id, @NonNull String name) {
        return new BatchWithStatus(new BatchInfo(id, name, ItemCategory.FOOD, 4, "units",
                DAY.plusDays(id), DateLabel.BEST_BY, 1, "Pantry", LocationType.PANTRY, false),
                DateStatus.NEAR_DATE);
    }

    @NonNull
    private static BatchWithStatus medicine(long id, @NonNull String name) {
        return new BatchWithStatus(new BatchInfo(id, name, ItemCategory.MEDICINE, 10, "tablets",
                DAY.plusDays(id), DateLabel.EXPIRES, 3, "First aid box", LocationType.FIRST_AID,
                false), DateStatus.NEAR_DATE);
    }
}

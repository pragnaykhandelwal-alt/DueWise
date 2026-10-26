package com.example.readyshelf.ui.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.InventoryFilter.DateOrder;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.shape.MaterialShapeDrawable;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory's top bar (2026-10-22): after a tap on a status tile on Home it has only a home icon,
 * and a tap on that icon closes Inventory, so Home shows again. Opened from Home's Inventory
 * tile, it keeps the back arrow and "Inventory". Since 2026-10-23 the bar is pine with a light
 * icon, and a status list starts with its status's tile as a header, in place of the status's
 * chip. The tests only open Inventory, read it, and change filter choices, which live only on the
 * screen; nothing is saved.
 */
@RunWith(AndroidJUnit4.class)
public class InventoryTopBarTest {

    @NonNull
    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void aStatusTile_opensInventoryWithOnlyAHomeIcon() {
        Intent fromTile = InventoryActivity.newStatusIntent(context(), DateStatus.PAST_DATE);
        try (ActivityScenario<InventoryActivity> scenario = ActivityScenario.launch(fromTile)) {
            scenario.onActivity(activity -> {
                MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
                assertTrue(TextUtils.isEmpty(toolbar.getTitle()));
                assertEquals(activity.getString(R.string.navigate_home),
                        String.valueOf(toolbar.getNavigationContentDescription()));
                // Only the top bar lost the word: the screen's own label stays.
                assertEquals(activity.getString(R.string.title_inventory),
                        String.valueOf(activity.getTitle()));
            });
        }
    }

    @Test
    public void aTapOnTheHomeIcon_closesInventory() {
        Intent fromTile = InventoryActivity.newStatusIntent(context(), DateStatus.NEAR_DATE);
        try (ActivityScenario<InventoryActivity> scenario = ActivityScenario.launch(fromTile)) {
            scenario.onActivity(activity -> {
                topBarButton(activity, R.string.navigate_home).performClick();
                assertTrue(activity.isFinishing());
            });
        }
    }

    @Test
    public void theInventoryTile_keepsTheBackArrowAndTheTitle() {
        Intent fromInventoryTile = new Intent(context(), InventoryActivity.class);
        try (ActivityScenario<InventoryActivity> scenario =
                     ActivityScenario.launch(fromInventoryTile)) {
            scenario.onActivity(activity -> {
                MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
                assertEquals(activity.getString(R.string.title_inventory),
                        String.valueOf(toolbar.getTitle()));
                assertEquals(activity.getString(R.string.navigate_back),
                        String.valueOf(toolbar.getNavigationContentDescription()));
            });
        }
    }

    @Test
    public void bothTopBars_arePineWithALightIcon() {
        Intent[] starts = {
                InventoryActivity.newStatusIntent(context(), DateStatus.PAST_DATE),
                new Intent(context(), InventoryActivity.class)};
        for (Intent start : starts) {
            try (ActivityScenario<InventoryActivity> scenario = ActivityScenario.launch(start)) {
                scenario.onActivity(activity -> {
                    MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
                    MaterialShapeDrawable background =
                            (MaterialShapeDrawable) toolbar.getBackground();
                    assertNotNull(background.getFillColor());
                    assertEquals(ContextCompat.getColor(activity, R.color.pine),
                            background.getFillColor().getDefaultColor());
                    assertEquals(Integer.valueOf(
                                    ContextCompat.getColor(activity, R.color.md_theme_onPrimary)),
                            toolbar.getNavigationIconTint());
                });
            }
        }
    }

    @Test
    public void theTopBarsButton_isCenteredUnderTheStatusBar() {
        Object[][] starts = {
                {InventoryActivity.newStatusIntent(context(), DateStatus.PAST_DATE),
                        R.string.navigate_home},
                {new Intent(context(), InventoryActivity.class), R.string.navigate_back}};
        for (Object[] start : starts) {
            try (ActivityScenario<InventoryActivity> scenario =
                         ActivityScenario.launch((Intent) start[0])) {
                scenario.onActivity(activity -> {
                    MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
                    View button = topBarButton(activity, (Integer) start[1]);
                    // The bar's own part starts under the status bar (its top padding).
                    int barTop = toolbar.getPaddingTop();
                    int barCenter = barTop + (toolbar.getHeight() - barTop
                            - toolbar.getPaddingBottom()) / 2;
                    int buttonCenter = (button.getTop() + button.getBottom()) / 2;
                    assertTrue("button center " + buttonCenter + ", bar center " + barCenter,
                            Math.abs(buttonCenter - barCenter) <= 2);
                });
            }
        }
    }

    @Test
    public void aStatusTile_opensItsListWithTheTileAsTheHeader() throws InterruptedException {
        Intent fromTile = InventoryActivity.newStatusIntent(context(), DateStatus.PAST_DATE);
        try (ActivityScenario<InventoryActivity> scenario = ActivityScenario.launch(fromTile)) {
            InventoryViewModel.State state = waitForState(scenario);
            assertTrue("the date rules give statuses", state.statusesKnown);
            scenario.onActivity(activity -> {
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.inventory_status_band).getVisibility());
                View tile = activity.findViewById(R.id.inventory_status_tile);
                String word = activity.getString(R.string.status_past_date);
                assertEquals(word, text(tile, R.id.tile_word));
                // The same number as the tile on Home: every batch that is not archived.
                assertEquals(String.valueOf(state.statusCount), text(tile, R.id.tile_count));
                assertEquals(View.GONE, tile.findViewById(R.id.tile_arrow).getVisibility());
                assertFalse(tile.isClickable());
                assertTrue(ViewCompat.isAccessibilityHeading(tile));
                // The header says the status, so no chip says it again.
                assertFalse(appliedChipTexts(activity).contains(word));
            });
        }
    }

    @Test
    public void theInventoryTile_opensInventoryWithoutAHeader() throws InterruptedException {
        Intent fromInventoryTile = new Intent(context(), InventoryActivity.class);
        try (ActivityScenario<InventoryActivity> scenario =
                     ActivityScenario.launch(fromInventoryTile)) {
            waitForState(scenario);
            scenario.onActivity(activity -> assertEquals(View.GONE,
                    activity.findViewById(R.id.inventory_status_band).getVisibility()));
        }
    }

    @Test
    public void clearAll_keepsTheHeadersStatus() throws InterruptedException {
        Intent fromTile = InventoryActivity.newStatusIntent(context(), DateStatus.NEAR_DATE);
        try (ActivityScenario<InventoryActivity> scenario = ActivityScenario.launch(fromTile)) {
            waitForState(scenario);
            // Two other filters, so that "Clear all" shows. Filter choices live only on the screen.
            scenario.onActivity(activity -> {
                ((MaterialButtonToggleGroup) activity.findViewById(R.id.inventory_order))
                        .check(R.id.inventory_order_latest);
                ((MaterialSwitch) activity.findViewById(R.id.inventory_show_archived))
                        .setChecked(true);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity ->
                    appliedChip(activity, R.string.filter_clear_all).performClick());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                InventoryViewModel viewModel =
                        new ViewModelProvider(activity).get(InventoryViewModel.class);
                assertEquals(DateStatus.NEAR_DATE, viewModel.getStatus());
                assertEquals(DateOrder.SOONEST_FIRST, viewModel.getDateOrder());
                assertFalse(viewModel.isShowArchived());
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.inventory_status_band).getVisibility());
            });
        }
    }

    /** Waits (up to 10 s) for the screen's first data, and returns it. */
    @NonNull
    private static InventoryViewModel.State waitForState(
            @NonNull ActivityScenario<InventoryActivity> scenario) throws InterruptedException {
        InventoryViewModel.State[] state = new InventoryViewModel.State[1];
        for (int i = 0; i < 100 && state[0] == null; i++) {
            scenario.onActivity(activity -> state[0] = new ViewModelProvider(activity)
                    .get(InventoryViewModel.class).getState().getValue());
            if (state[0] == null) {
                Thread.sleep(100);
            }
        }
        assertNotNull("Inventory's data didn't arrive", state[0]);
        return state[0];
    }

    @NonNull
    private static String text(@NonNull View parent, int id) {
        return ((TextView) parent.findViewById(id)).getText().toString();
    }

    /** The words of the chips under the search box. */
    @NonNull
    private static List<String> appliedChipTexts(@NonNull InventoryActivity activity) {
        ViewGroup chips = activity.findViewById(R.id.inventory_applied_filters);
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < chips.getChildCount(); i++) {
            texts.add(((TextView) chips.getChildAt(i)).getText().toString());
        }
        return texts;
    }

    /** The chip under the search box with these words. */
    @NonNull
    private static View appliedChip(@NonNull InventoryActivity activity, @StringRes int words) {
        ViewGroup chips = activity.findViewById(R.id.inventory_applied_filters);
        String wanted = activity.getString(words);
        for (int i = 0; i < chips.getChildCount(); i++) {
            TextView chip = (TextView) chips.getChildAt(i);
            if (wanted.contentEquals(chip.getText())) {
                return chip;
            }
        }
        throw new AssertionError("No chip \"" + wanted + "\"");
    }

    /** The top bar's one button with this spoken label. (The toolbar doesn't hand it out.) */
    @NonNull
    private static View topBarButton(@NonNull InventoryActivity activity, @StringRes int label) {
        ArrayList<View> found = new ArrayList<>();
        activity.findViewById(R.id.toolbar).findViewsWithText(found, activity.getString(label),
                View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        assertEquals(1, found.size());
        return found.get(0);
    }
}

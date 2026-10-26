package com.example.readyshelf.ui.common;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.ui.home.HomeActivity;
import com.example.readyshelf.ui.inventory.InventoryActivity;
import com.example.readyshelf.ui.purchase.PurchaseListActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ScreenSetup's edges (2026-10-23). Turned sideways, the pine top bar and the bands reach the
 * screen's sides (the camera cutout's too) while their contents keep clear of them; a navigation
 * bar at a side (3-button navigation) is the exception: the screen stops at it, so its buttons
 * stay on the page's light color. In portrait every part has its own padding again. The tests
 * hand the screens made-up insets instead of turning the phone. They only open screens; nothing
 * is saved.
 */
@RunWith(AndroidJUnit4.class)
public class ScreenSetupInsetsTest {

    // Gesture navigation, sideways: cutout-like insets on both sides (40 and 50 px), the status
    // bar 30 px, the gesture bar 20 px at the bottom.
    private static final WindowInsetsCompat GESTURE_SIDEWAYS = insets(
            Insets.of(40, 0, 50, 0), Insets.of(0, 30, 0, 0), Insets.of(0, 0, 0, 20));
    // 3-button navigation, sideways: the camera cutout on the left (40 px), the navigation bar on
    // the right (50 px), the status bar 30 px.
    private static final WindowInsetsCompat BUTTONS_SIDEWAYS = insets(
            Insets.of(40, 0, 0, 0), Insets.of(0, 30, 0, 0), Insets.of(0, 0, 50, 0));
    // Portrait: no side insets.
    private static final WindowInsetsCompat PORTRAIT = insets(
            Insets.NONE, Insets.of(0, 30, 0, 0), Insets.of(0, 0, 0, 20));

    @NonNull
    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void statusList_bandReachesTheSides_contentsKeepClear() {
        Intent fromTile = InventoryActivity.newStatusIntent(context(), DateStatus.PAST_DATE);
        try (ActivityScenario<InventoryActivity> scenario = ActivityScenario.launch(fromTile)) {
            scenario.onActivity(activity -> checkFullWidthScreen(activity,
                    R.id.inventory_status_band, R.id.inventory_below_band, true));
        }
    }

    @Test
    public void home_bandReachesTheSides_contentsKeepClear() {
        // Not a launcher start, so Home doesn't close itself when it isn't the task's first screen.
        Intent home = new Intent(context(), HomeActivity.class);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(home)) {
            scenario.onActivity(activity ->
                    checkFullWidthScreen(activity, R.id.home_tiles, R.id.home_below_band, false));
        }
    }

    @Test
    public void purchaseList_contentKeepsClearOfTheSides() {
        Intent purchaseList = new Intent(context(), PurchaseListActivity.class);
        try (ActivityScenario<PurchaseListActivity> scenario =
                     ActivityScenario.launch(purchaseList)) {
            scenario.onActivity(activity -> {
                ViewGroup root = activity.findViewById(R.id.main);
                View toolbar = activity.findViewById(R.id.toolbar);
                View body = root.getChildAt(root.getChildCount() - 1);

                dispatch(root, GESTURE_SIDEWAYS);
                assertPadding(root, 0, 0, 0, 20);
                assertSides(toolbar, 40, 50);
                assertEquals(30, toolbar.getPaddingTop());
                assertSides(body, 40, 50);

                dispatch(root, BUTTONS_SIDEWAYS);
                assertPadding(root, 0, 0, 50, 0);
                assertSides(toolbar, 40, 0);
                assertSides(body, 40, 0);

                dispatch(root, PORTRAIT);
                assertPadding(root, 0, 0, 0, 20);
                assertSides(toolbar, 0, 0);
                assertEquals(30, toolbar.getPaddingTop());
                assertSides(body, 0, 0);
            });
        }
    }

    /**
     * Home's and Inventory's body reaches the sides (no padding of its own); the band and the part
     * below it keep their 16 dp plus the insets, and Inventory's floating button its 16 dp margins
     * plus the insets (Home has none since 2026-10-23: Add batch is a tile there). A side
     * navigation bar stays outside the screen. Without side insets every value is its own again.
     */
    private static void checkFullWidthScreen(@NonNull Activity activity, @IdRes int band,
                                             @IdRes int belowBand, boolean hasAddButton) {
        ViewGroup root = activity.findViewById(R.id.main);
        View toolbar = activity.findViewById(R.id.toolbar);
        View body = root.getChildAt(root.getChildCount() - 1);
        int padding = activity.getResources().getDimensionPixelSize(R.dimen.screen_padding);
        int margin = activity.getResources().getDimensionPixelSize(R.dimen.space_medium);
        View fab = activity.findViewById(R.id.fab_add_batch);
        assertEquals("a floating Add batch button", hasAddButton, fab != null);

        dispatch(root, GESTURE_SIDEWAYS);
        assertPadding(root, 0, 0, 0, 20);
        assertSides(toolbar, 40, 50);
        assertEquals(30, toolbar.getPaddingTop());
        assertSides(body, 0, 0);
        assertSides(activity.findViewById(band), padding + 40, padding + 50);
        assertSides(activity.findViewById(belowBand), padding + 40, padding + 50);
        if (fab != null) {
            assertMargins(fab, margin + 40, margin + 50);
        }

        dispatch(root, BUTTONS_SIDEWAYS);
        assertPadding(root, 0, 0, 50, 0);
        assertSides(toolbar, 40, 0);
        assertSides(body, 0, 0);
        assertSides(activity.findViewById(band), padding + 40, padding);
        assertSides(activity.findViewById(belowBand), padding + 40, padding);
        if (fab != null) {
            assertMargins(fab, margin + 40, margin);
        }

        dispatch(root, PORTRAIT);
        assertPadding(root, 0, 0, 0, 20);
        assertSides(toolbar, 0, 0);
        assertEquals(30, toolbar.getPaddingTop());
        assertSides(body, 0, 0);
        assertSides(activity.findViewById(band), padding, padding);
        assertSides(activity.findViewById(belowBand), padding, padding);
        if (fab != null) {
            assertMargins(fab, margin, margin);
        }
    }

    @NonNull
    private static WindowInsetsCompat insets(@NonNull Insets cutout, @NonNull Insets statusBar,
                                             @NonNull Insets navigationBar) {
        return new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.displayCutout(), cutout)
                .setInsets(WindowInsetsCompat.Type.statusBars(), statusBar)
                .setInsets(WindowInsetsCompat.Type.navigationBars(), navigationBar)
                .build();
    }

    private static void dispatch(@NonNull View root, @NonNull WindowInsetsCompat insets) {
        ViewCompat.dispatchApplyWindowInsets(root, insets);
    }

    private static void assertPadding(@NonNull View view, int left, int top, int right,
                                      int bottom) {
        assertEquals("left padding", left, view.getPaddingLeft());
        assertEquals("top padding", top, view.getPaddingTop());
        assertEquals("right padding", right, view.getPaddingRight());
        assertEquals("bottom padding", bottom, view.getPaddingBottom());
    }

    private static void assertSides(@NonNull View view, int left, int right) {
        assertEquals("left padding", left, view.getPaddingLeft());
        assertEquals("right padding", right, view.getPaddingRight());
    }

    private static void assertMargins(@NonNull View view, int left, int right) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        assertEquals("left margin", left, params.leftMargin);
        assertEquals("right margin", right, params.rightMargin);
    }
}

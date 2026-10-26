package com.example.readyshelf.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.R;
import com.example.readyshelf.ui.batch.BatchEditActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Home's "Add batch" tile (2026-10-23, the student's choice): it is the grid's last tile, in place
 * of the floating button, and a tap on it opens the batch form. The grid never has an empty cell,
 * because a tile alone in the last row fills it. Home has 0 to 3 status tiles and 4 shortcut
 * tiles, so its grid has 4 to 7 tiles. The tests only open screens; nothing is saved.
 */
@RunWith(AndroidJUnit4.class)
public class HomeTilesTest {

    // The width of the made-up grids, in pixels (about a phone's).
    private static final int GRID_WIDTH = 1000;

    @NonNull
    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /** Not a launcher start, so Home doesn't close itself when it isn't the task's first screen. */
    @NonNull
    private static Intent home() {
        return new Intent(context(), HomeActivity.class);
    }

    @Test
    public void addBatch_isTheLastTile_andHomeHasNoFloatingButton() {
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(home())) {
            scenario.onActivity(activity -> {
                assertNull(activity.findViewById(R.id.fab_add_batch));
                View addBatch = activity.findViewById(R.id.home_add_batch);
                assertNotNull(addBatch);
                assertEquals(activity.getString(R.string.home_add_batch),
                        String.valueOf(addBatch.getContentDescription()));
                ViewGroup grid = activity.findViewById(R.id.home_tiles);
                ViewGroup lastRow = (ViewGroup) grid.getChildAt(grid.getChildCount() - 1);
                assertSame(addBatch, lastRow.getChildAt(lastRow.getChildCount() - 1));
            });
        }
    }

    @Test
    public void aTapOnAddBatch_opensTheBatchForm() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor =
                instrumentation.addMonitor(BatchEditActivity.class.getName(), null, false);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(home())) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.home_add_batch).performClick());
            Activity form = instrumentation.waitForMonitorWithTimeout(monitor, 10_000);
            assertNotNull("the batch form opened", form);
            // Closed at once: nothing was typed, so nothing is saved.
            instrumentation.runOnMainSync(form::finish);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void twoTilesInARow_everyRowIsFull() {
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(home())) {
            scenario.onActivity(activity -> {
                for (int count = 4; count <= 7; count++) {
                    checkEveryRowIsFull(activity, count, 2);
                }
            });
        }
    }

    @Test
    public void oneTileInARow_everyTileFillsItsRow() {
        // With large text Home puts one tile in a row (LargeText.stackRows).
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(home())) {
            scenario.onActivity(activity -> {
                for (int count = 4; count <= 7; count++) {
                    checkEveryRowIsFull(activity, count, 1);
                }
            });
        }
    }

    /**
     * Fills a made-up grid, set up like Home's (vertical, every row as tall as the tallest one),
     * with {@code count} shortcut tiles, {@code perRow} in a row, and checks that no row has an
     * empty cell: a row's tiles and the gaps between them take its whole width. Only the last row
     * may have fewer tiles, and two tiles in a row share it equally.
     */
    private static void checkEveryRowIsFull(@NonNull Activity activity, int count, int perRow) {
        int gap = activity.getResources().getDimensionPixelSize(R.dimen.space_small);
        LinearLayout grid = new LinearLayout(activity);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setMeasureWithLargestChildEnabled(true);
        List<View> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tiles.add(activity.getLayoutInflater().inflate(R.layout.item_action_tile, grid, false));
        }

        HomeActivity.fillGrid(grid, tiles, perRow, gap);
        // As Home's scroll view measures it: a fixed width, and as tall as it needs to be.
        grid.measure(View.MeasureSpec.makeMeasureSpec(GRID_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        grid.layout(0, 0, grid.getMeasuredWidth(), grid.getMeasuredHeight());

        String what = count + " tiles, " + perRow + " in a row";
        int rows = (count + perRow - 1) / perRow;
        assertEquals(what + ": rows", rows, grid.getChildCount());
        int rowHeight = grid.getChildAt(0).getHeight();
        assertTrue(what + ": row height " + rowHeight, rowHeight > 0);
        int next = 0;
        for (int r = 0; r < rows; r++) {
            ViewGroup row = (ViewGroup) grid.getChildAt(r);
            String where = what + ", row " + (r + 1);
            int inRow = row.getChildCount();
            assertEquals(where + ": tiles", Math.min(perRow, count - perRow * r), inRow);
            assertEquals(where + ": height", rowHeight, row.getHeight());
            int widths = 0;
            for (int c = 0; c < inRow; c++) {
                View tile = row.getChildAt(c);
                assertSame(where + ": tile order", tiles.get(next++), tile);
                assertEquals(where + ": tile height", rowHeight, tile.getHeight());
                widths += tile.getWidth();
            }
            // No empty cell: the tiles and the gaps between them take the whole row.
            assertEquals(where + ": width", GRID_WIDTH, widths + gap * (inRow - 1));
            if (inRow == 2) {
                int difference = row.getChildAt(0).getWidth() - row.getChildAt(1).getWidth();
                assertTrue(where + ": equal widths", Math.abs(difference) <= 1);
            }
        }
    }
}

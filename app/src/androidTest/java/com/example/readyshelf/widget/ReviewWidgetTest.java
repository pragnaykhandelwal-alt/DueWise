package com.example.readyshelf.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.DateThresholds;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The home-screen widget, on the emulator: Android offers it with its settings, each state's words
 * name no items (since 2026-10-23 they can give one number, Home's tiles added up), the whole
 * widget is the tap target, and while no widget is placed a refresh reads nothing. Placing a
 * widget needs the home screen itself, so that is checked by hand.
 *
 * <p>Run these tests only on the emulator, never on the household phone (see TOOLCHAIN.md).
 */
@RunWith(AndroidJUnit4.class)
public class ReviewWidgetTest {

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void theWidgetIsOffered_withItsSettings() {
        List<AppWidgetProviderInfo> providers = AppWidgetManager.getInstance(context)
                .getInstalledProvidersForPackage(context.getPackageName(), null);

        assertEquals(1, providers.size());
        AppWidgetProviderInfo info = providers.get(0);
        assertEquals(new ComponentName(context, ReviewWidgetProvider.class), info.provider);
        assertEquals(context.getString(R.string.widget_name),
                info.loadLabel(context.getPackageManager()));
        assertEquals(R.layout.widget_review, info.initialLayout);
        assertEquals(21600000, info.updatePeriodMillis);
        assertEquals(AppWidgetProviderInfo.RESIZE_BOTH, info.resizeMode);
        assertEquals(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN, info.widgetCategory);
        assertNull("The widget has no setup screen", info.configure);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertEquals(3, info.targetCellWidth);
            assertEquals(1, info.targetCellHeight);
            assertEquals(R.layout.widget_review, info.previewLayout);
            assertEquals(context.getString(R.string.widget_description),
                    String.valueOf(info.loadDescription(context)));
        }
    }

    @Test
    public void eachLine_namesNoItems_andTheWholeWidgetIsTheTapTarget() {
        ReviewWidget widget = new ReviewWidget(context, Collections::emptyList,
                DateThresholds::defaults, listener -> { }, null);
        // Since 2026-10-23 the line can give one number: Home's tiles added up.
        Map<ReviewWidgetState, String> lines = new LinkedHashMap<>();
        lines.put(ReviewWidgetState.UNKNOWN, context.getString(R.string.reminder_daily_text));
        lines.put(ReviewWidgetState.NOTHING_TO_REVIEW,
                context.getString(R.string.home_needs_review_empty));
        // Only dates to review: they have no tile, so the line has no number.
        lines.put(ReviewWidgetState.itemsToReview(0),
                context.getString(R.string.reminder_daily_title));
        lines.put(ReviewWidgetState.itemsToReview(1), "1 household item needs review");
        lines.put(ReviewWidgetState.itemsToReview(12), "12 household items need review");
        for (Map.Entry<ReviewWidgetState, String> line : lines.entrySet()) {
            ReviewWidgetState state = line.getKey();
            RemoteViews views = widget.views(state);
            View[] root = new View[1];
            // Applying the views also checks that the layout uses only views widgets allow.
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    () -> root[0] = views.apply(context, new FrameLayout(context)));

            TextView name = root[0].findViewById(R.id.widget_app_name);
            TextView lineView = root[0].findViewById(R.id.widget_review_line);
            String words = String.valueOf(lineView.getText());
            assertEquals(context.getString(R.string.app_name), String.valueOf(name.getText()));
            assertEquals(state.toString(), line.getValue(), words);
            assertEquals(state.toString(), ReviewWidget.lineOf(context, state), words);
            // The only number is Home's tiles added up, and only when it is above 0.
            assertEquals("Number in: " + words, state.getNumber() > 0,
                    words.matches(".*\\d.*"));
            assertEquals(android.R.id.background, root[0].getId());
            assertTrue(state.toString(), root[0].hasOnClickListeners());
        }
    }

    @Test
    public void withNoWidgetPlaced_aRefreshReadsNothing() {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        assumeTrue("Remove ReadyShelf's widget from the emulator's home screen for this test",
                manager.getAppWidgetIds(new ComponentName(context, ReviewWidgetProvider.class))
                        .length == 0);
        AtomicInteger reads = new AtomicInteger();
        ReviewWidget widget = new ReviewWidget(context,
                () -> {
                    reads.incrementAndGet();
                    return Collections.emptyList();
                },
                DateThresholds::defaults,
                listener -> fail("Nothing should listen while no widget is placed"),
                (date, today, nearDateDays) -> {
                    throw new AssertionError("The date rules should not be asked");
                });

        widget.refreshNow(true);

        assertEquals(0, reads.get());
    }
}

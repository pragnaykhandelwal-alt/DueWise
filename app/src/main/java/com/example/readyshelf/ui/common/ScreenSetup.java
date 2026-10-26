package com.example.readyshelf.ui.common;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/** Setup steps that every screen shares. */
public final class ScreenSetup {

    /**
     * A layout tag (android:tag) for a direct child of the root that reaches the screen's sides,
     * so a pine band inside it does too. Inside it, only the views tagged {@link #INSET_SIDES} or
     * {@link #INSET_SIDES_MARGIN} keep clear of the sides.
     */
    static final String FULL_WIDTH = "fullWidth";
    /** A layout tag inside a {@link #FULL_WIDTH} part: the side insets are added to its padding. */
    static final String INSET_SIDES = "insetSides";
    /**
     * A layout tag inside a {@link #FULL_WIDTH} part: the side insets are added to its left and
     * right margins (set with android:layout_margin). For the floating "Add batch" button, which
     * manages its own padding.
     */
    static final String INSET_SIDES_MARGIN = "insetSidesMargin";

    /** A view that keeps clear of the screen's sides, with its own side padding or margins. */
    private static final class SideInset {
        @NonNull final View view;
        final boolean margin;
        final int left;
        final int right;

        SideInset(@NonNull View view, boolean margin) {
            this.view = view;
            this.margin = margin;
            if (margin) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                left = params.leftMargin;
                right = params.rightMargin;
            } else {
                left = view.getPaddingLeft();
                right = view.getPaddingRight();
            }
        }

        /** Its own padding or margins, plus the screen's side insets (0 in portrait). */
        void apply(@NonNull Insets sides) {
            if (margin) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                if (params.leftMargin != left + sides.left
                        || params.rightMargin != right + sides.right) {
                    params.leftMargin = left + sides.left;
                    params.rightMargin = right + sides.right;
                    view.setLayoutParams(params);
                }
            } else {
                view.setPadding(left + sides.left, view.getPaddingTop(), right + sides.right,
                        view.getPaddingBottom());
            }
        }
    }

    private ScreenSetup() {
        // Only static methods, so no objects of this class are made.
    }

    /**
     * Shows a screen's layout and sets up the parts that every screen has.
     *
     * <p>The app draws behind the status bar and the navigation bar ("edge to edge"). Since
     * 2026-10-23 (the student's design B1) every screen's top bar is pine (ReadyShelf.TopBar): it
     * reaches up behind the status bar, whose icons are light, and grows by the status bar's
     * height, so its title stays where it was. The root view (id "main") gets padding at the
     * bottom, so nothing is hidden behind the navigation bar or the keyboard. If the layout's
     * toolbar has a back arrow, tapping it closes the screen.
     *
     * <p>At the sides (in landscape: the camera cutout), the top bar and each other part of the
     * screen reach the edges, while their contents keep clear of them: the side insets are added
     * to their padding. A part tagged {@link #FULL_WIDTH} (Home's and Inventory's) keeps no
     * padding of its own, so its pine band reaches the edges; inside it the views tagged
     * {@link #INSET_SIDES} or {@link #INSET_SIDES_MARGIN} keep clear instead. A navigation bar at
     * a side (3-button navigation, turned sideways) is the exception: the screen stops at it, so
     * its buttons stay on the page's light color. In portrait the side insets are 0, so nothing
     * moves.
     *
     * <p>While the separate sample data is open, the "Sample data" label goes right under the
     * toolbar, with "Close sample data" under its words (SampleDataClose). It is outside the part
     * that scrolls, so it is always in view. It always matches the data the screen shows: the
     * screen's ViewModel keeps the data set it was made with, and every switch between the
     * household's records and the sample data makes all screens again (SampleDataClose).
     */
    public static void apply(@NonNull AppCompatActivity activity, @LayoutRes int layoutId) {
        // The app is always light (since 2026-10-22), so the navigation bar's icons are dark,
        // even when the phone itself is in dark mode. The status bar is over a pine top bar on
        // every screen (since 2026-10-23), so its icons are light.
        EdgeToEdge.enable(activity, SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        activity.setContentView(layoutId);

        MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.setNavigationOnClickListener(view -> activity.finish());
        }

        ViewGroup root = activity.findViewById(R.id.main);
        if (ReadyShelfApp.containerOf(activity.getApplication()).dataSets.isSampleOpen()) {
            View label = activity.getLayoutInflater()
                    .inflate(R.layout.view_sample_data_label, root, false);
            label.findViewById(R.id.sample_data_close)
                    .setOnClickListener(view -> SampleDataClose.confirm(activity));
            root.addView(label, root.indexOfChild(toolbar) + 1);
        }

        // The views that keep clear of the sides, with their own padding and margins, found once.
        List<SideInset> insetViews = new ArrayList<>();
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (FULL_WIDTH.equals(child.getTag())) {
                findTagged(child, insetViews);
            } else {
                insetViews.add(new SideInset(child, false));
            }
        }
        // The toolbar's own height, before the status bar's height is added to it.
        int toolbarHeight = toolbar.getMinimumHeight();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            // A navigation bar at a side (3-button navigation, with the phone turned sideways)
            // keeps the page's light color under its buttons, so they stay easy to see (the
            // student's choice on 2026-10-23): the screen stops at it. The pine reaches every
            // other side, for example the camera cutout's, and only the contents keep clear.
            view.setPadding(navigation.left, 0, navigation.right,
                    Math.max(bars.bottom, keyboard.bottom));
            Insets sides = Insets.of(bars.left - navigation.left, 0,
                    bars.right - navigation.right, 0);
            for (SideInset insetView : insetViews) {
                insetView.apply(sides);
            }
            // The top bar reaches up behind the status bar: it gets the status bar's height as
            // padding and grows by as much, so its logo or title stays where it was.
            toolbar.setPadding(toolbar.getPaddingLeft(), bars.top, toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom());
            toolbar.setMinimumHeight(toolbarHeight + bars.top);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Adds the views inside {@code view} that are tagged to keep clear of the sides. */
    private static void findTagged(@NonNull View view, @NonNull List<SideInset> sides) {
        Object tag = view.getTag();
        if (INSET_SIDES.equals(tag)) {
            sides.add(new SideInset(view, false));
        } else if (INSET_SIDES_MARGIN.equals(tag)) {
            sides.add(new SideInset(view, true));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findTagged(group.getChildAt(i), sides);
            }
        }
    }

    /** Sets up a list that sits inside a screen that scrolls as a whole. */
    public static void setUpList(@NonNull RecyclerView list,
                                 @NonNull RecyclerView.Adapter<?> adapter) {
        list.setLayoutManager(new LinearLayoutManager(list.getContext()));
        list.setAdapter(adapter);
        // The whole screen scrolls, so the list doesn't scroll by itself.
        list.setNestedScrollingEnabled(false);
        // Without this, the screen can jump down to the list when it opens.
        list.setFocusableInTouchMode(false);
        // No divider lines: each batch is its own card, with space between the cards.
    }
}

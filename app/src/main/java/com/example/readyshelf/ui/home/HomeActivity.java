package com.example.readyshelf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.ReviewCounts;
import com.example.readyshelf.domain.RuleRunner;
import com.example.readyshelf.ui.batch.BatchEditActivity;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.LargeText;
import com.example.readyshelf.ui.common.MessageText;
import com.example.readyshelf.ui.common.MoreText;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.common.StatusTile;
import com.example.readyshelf.ui.common.SwapDiagram;
import com.example.readyshelf.ui.common.SwapText;
import com.example.readyshelf.ui.inventory.InventoryActivity;
import com.example.readyshelf.ui.purchase.PurchaseListActivity;
import com.example.readyshelf.ui.settings.SettingsActivity;
import com.example.readyshelf.ui.swap.SwapReviewActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The first screen: one grid of tiles (how many batches need review, by status, and the
 * shortcuts Inventory, Purchase list, Settings, and Add batch), a note when nothing needs review
 * or the date rules can't run, and recommended swaps. A status tile opens Inventory with its
 * batches, medicine too. Since 2026-10-22 (the student's choice) Home has no "Items need review"
 * heading, no "Show the review list" tile, and no review lists. Since 2026-10-23 (the student's
 * choice) "Add batch" is the grid's last tile instead of a floating button.
 */
public class HomeActivity extends AppCompatActivity {

    private static final String EXTRA_SHOW_REVIEW = "com.example.readyshelf.SHOW_REVIEW";
    private static final String REVIEW_SCHEME = "readyshelf-review";
    // Home's grid shows its tiles two in a row (one with large text).
    private static final int TILES_PER_ROW = 2;

    /**
     * Opens Home at the top, where the status tiles are. The reminder notifications and the
     * home-screen widget use it.
     * SINGLE_TOP: a Home that is already in front gets it through onNewIntent. No CLEAR_TOP,
     * so a form that is open (for example Add batch) is kept, and Back returns to it.
     *
     * <p>The data is new each time. Android compares a tapped intent with the intent that
     * started ReadyShelf's task (extras don't count). If they were the same, Android would only
     * bring the task to the front, on the screen that was open, instead of opening Home.
     */
    @NonNull
    public static Intent newReviewIntent(@NonNull Context context) {
        return new Intent(context, HomeActivity.class)
                .setData(Uri.fromParts(REVIEW_SCHEME,
                        String.valueOf(System.currentTimeMillis()), null))
                .putExtra(EXTRA_SHOW_REVIEW, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    private HomeViewModel viewModel;
    private NestedScrollView scrollView;
    // The grid with all of Home's tiles.
    private LinearLayout tileGrid;
    // Inventory, Purchase list, Settings, and Add batch: always the last tiles of the grid.
    private final List<View> shortcutTiles = new ArrayList<>();
    // The status counts whose tiles are on screen; null until the grid is first filled. The grid
    // is rebuilt only when this changes, so TalkBack doesn't lose its place each time the
    // database sends new values.
    @Nullable private List<ReviewCounts.StatusCount> shownCounts;
    private TextView sampleBanner;
    private TextView emptyText;
    // "Nothing needs review right now.", or why the date rules gave no counts.
    private TextView reviewMessage;
    private TextView swapsMessage;
    // "More about this" under the swap note, and the text it shows: only while swaps are shown.
    private View swapsMoreGroup;
    private View swapsMoreText;
    private LinearLayout swapsList;
    // The swaps whose cards are on screen. Cards are rebuilt only when the swaps change, so
    // TalkBack doesn't lose its place each time the database sends new values.
    @Nullable private List<RuleRunner.ShownSwap> shownSwaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Opened from the launcher icon while ReadyShelf's task already has screens (a reminder
        // tap started that task): Android would put a second Home on top. Close this one, so
        // the household sees the task as it was.
        if (!isTaskRoot() && isLauncherStart(getIntent())) {
            finish();
            return;
        }
        ScreenSetup.apply(this, R.layout.activity_home);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        scrollView = findViewById(R.id.home_scroll);
        // A reminder or widget tap opened this Home. It starts at the top, where the tiles are.
        // (After a rotation this is the same Home again, so only a new start counts.)
        if (savedInstanceState == null
                && getIntent().getBooleanExtra(EXTRA_SHOW_REVIEW, false)) {
            viewModel.onReviewIntent();
        }
        tileGrid = findViewById(R.id.home_tiles);
        // The shortcut tiles are made once. showTiles puts them in the grid, after the status
        // tiles.
        shortcutTiles.add(makeActionTile(R.id.home_inventory, R.drawable.ic_inventory,
                R.string.home_inventory,
                view -> startActivity(new Intent(this, InventoryActivity.class))));
        shortcutTiles.add(makeActionTile(R.id.home_purchase_list, R.drawable.ic_shopping_cart,
                R.string.home_purchase_list,
                view -> startActivity(new Intent(this, PurchaseListActivity.class))));
        shortcutTiles.add(makeActionTile(R.id.home_settings, R.drawable.ic_settings,
                R.string.home_settings,
                view -> startActivity(new Intent(this, SettingsActivity.class))));
        // Home's main action, last (the student's choice on 2026-10-23): it takes the cell next
        // to Settings, or the whole last row when it is alone there (fillGrid).
        shortcutTiles.add(makeActionTile(R.id.home_add_batch, R.drawable.ic_add,
                R.string.home_add_batch,
                view -> startActivity(BatchEditActivity.newAddIntent(this))));
        // Until the first counts arrive, the grid has only the shortcut tiles.
        showTiles(Collections.emptyList());
        sampleBanner = findViewById(R.id.home_sample_banner);
        emptyText = findViewById(R.id.home_empty);
        reviewMessage = findViewById(R.id.home_review_message);
        swapsMessage = findViewById(R.id.home_swaps_message);
        swapsMoreGroup = findViewById(R.id.home_swaps_more_group);
        swapsMoreText = findViewById(R.id.home_swaps_more);
        MoreText.setUp(findViewById(R.id.home_swaps_more_toggle), swapsMoreText, savedInstanceState);
        swapsList = findViewById(R.id.home_swaps_list);

        viewModel.getState().observe(this, this::show);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshToday();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Whether the swaps' "More about this" is open, so a rotation keeps it. (Null when this
        // copy of Home closed itself in onCreate.)
        if (swapsMoreText != null) {
            MoreText.save(outState, swapsMoreText);
        }
    }

    /**
     * Makes one of the grid's shortcut tiles (item_action_tile.xml): its pine icon in a cream
     * circle, ">" at the top end, and its word, which is also its spoken label. The tile keeps its
     * id, so screen dumps can find it.
     */
    @NonNull
    private View makeActionTile(@IdRes int id, @DrawableRes int icon, @StringRes int word,
                                @NonNull View.OnClickListener onClick) {
        View tile = getLayoutInflater().inflate(R.layout.item_action_tile, tileGrid, false);
        tile.setId(id);
        ImageView iconView = tile.findViewById(R.id.action_tile_icon);
        iconView.setImageResource(icon);
        ImageView arrow = tile.findViewById(R.id.action_tile_arrow);
        arrow.setImageResource(R.drawable.ic_chevron_right);
        TextView wordView = tile.findViewById(R.id.action_tile_word);
        wordView.setText(word);
        tile.setContentDescription(getString(word));
        tile.setOnClickListener(onClick);
        return tile;
    }

    private static boolean isLauncherStart(@NonNull Intent intent) {
        return Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER);
    }

    /** A reminder notification or the widget was tapped while Home was already in front. */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra(EXTRA_SHOW_REVIEW, false)) {
            viewModel.onReviewIntent();
            // Back to the top, where the tiles are.
            scrollView.scrollTo(0, 0);
        }
    }

    private void show(HomeViewModel.State state) {
        showTiles(state.reviewCounts);
        MessageText.show(reviewMessage, state.reviewMessage, state.reviewError);
        // The banner is for sample items that an earlier version added to the household's own
        // records. While the separate sample data is open, the "Sample data" label says it.
        sampleBanner.setVisibility(state.hasSampleData && !state.sampleDataOpen
                ? View.VISIBLE : View.GONE);
        emptyText.setVisibility(state.hasBatches ? View.GONE : View.VISIBLE);

        showSwaps(state.swaps);
        MessageText.show(swapsMessage, state.swapsMessage, state.swapsError);
        // "More about this" only goes with swap cards: not with "No swaps to suggest right now."
        // or an error (the rules give no swaps then).
        swapsMoreGroup.setVisibility(state.swaps.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Home's grid (the student's choice on 2026-10-19): one tile per status, "Near date" first,
     * then Inventory, Purchase list, Settings, and Add batch. Two tiles in a row, or one with
     * large text (LargeText). Every row is as tall as the tallest one (measureWithLargestChild in
     * activity_home.xml), and two tiles in a row share it equally, so every icon sits in the same
     * two columns. With an odd number of tiles, the last one, Add batch, fills its row (since
     * 2026-10-23, the student's choice), so the grid has no empty cell.
     *
     * <p>Each status tile shows the count and the status word, for example "2" and "Date
     * passed", and TalkBack reads it as one line, "Date passed: 2 batches". A tap opens Inventory
     * with only those batches, medicine too. "Date needs review" gets no tile (the student's
     * choice on 2026-10-19, kept on 2026-10-22): its batches are in Inventory's "Date needs
     * review" filter. With nothing to review, or with the date rules missing or stopped, the grid
     * has only the four shortcut tiles, and the note below it says why.
     */
    private void showTiles(@NonNull List<ReviewCounts.StatusCount> counts) {
        List<ReviewCounts.StatusCount> tileCounts = new ArrayList<>();
        // The same rule as the number on the app icon and the widget (ReviewCounts.tilesTotal).
        for (ReviewCounts.StatusCount count : counts) {
            if (ReviewCounts.hasTile(count.getStatus())) {
                tileCounts.add(count);
            }
        }
        if (Objects.equals(tileCounts, shownCounts)) {
            return;
        }
        shownCounts = tileCounts;
        List<View> tiles = new ArrayList<>();
        LayoutInflater inflater = getLayoutInflater();
        for (ReviewCounts.StatusCount count : tileCounts) {
            tiles.add(makeTile(inflater, tileGrid, count));
        }
        tiles.addAll(shortcutTiles);
        int perRow = LargeText.stackRows(getResources()) ? 1 : TILES_PER_ROW;
        int gap = getResources().getDimensionPixelSize(R.dimen.space_small);
        fillGrid(tileGrid, tiles, perRow, gap);
    }

    /**
     * Puts {@code tiles} into {@code grid}, {@code perRow} in a row, {@code gap} pixels apart. A
     * tile alone in the last row fills it (since 2026-10-23, the student's choice): like every
     * tile, it has a width of 0 and a weight, so it takes the whole row. Until then an empty
     * filler as tall as the row kept it half wide, which left an empty cell next to it.
     */
    @VisibleForTesting
    static void fillGrid(@NonNull LinearLayout grid, @NonNull List<View> tiles, int perRow,
                         int gap) {
        // The tiles that are kept, not made again, leave their old rows first.
        for (View tile : tiles) {
            detachFromRow(tile);
        }
        grid.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(grid.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                // The weight lets measureWithLargestChild make this row as tall as the tallest.
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f);
                rowParams.topMargin = i == 0 ? 0 : gap;
                grid.addView(row, rowParams);
            }
            View tile = tiles.get(i);
            ((LinearLayout.LayoutParams) tile.getLayoutParams())
                    .setMarginStart(i % perRow == 0 ? 0 : gap);
            row.addView(tile);
        }
    }

    /** Takes a tile that is kept between rebuilds out of its old row, so a new row can take it. */
    private static void detachFromRow(@NonNull View tile) {
        ViewGroup oldRow = (ViewGroup) tile.getParent();
        if (oldRow != null) {
            oldRow.removeView(tile);
        }
    }

    /**
     * One status tile (StatusTile): the status icon in a circle of the status's label colors, the
     * count in the status's accent color, a small arrow, and the status word, read by TalkBack as
     * one line, for example "Date passed: 2 batches". A tap opens Inventory with that status
     * chosen, so it lists the same batches that the tile counts: the batches that are not
     * archived, medicine too. Since 2026-10-23 that list starts with the same tile as its header.
     */
    @NonNull
    private View makeTile(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent,
                          @NonNull ReviewCounts.StatusCount count) {
        View tile = inflater.inflate(R.layout.item_status_tile, parent, false);
        DateStatus status = count.getStatus();
        StatusTile.bind(tile, status, count.getCount());
        tile.setOnClickListener(
                view -> startActivity(InventoryActivity.newStatusIntent(this, status)));
        // TalkBack then says "Double-tap to show these batches in Inventory" instead of
        // "Double-tap to activate". The tap itself stays the click above.
        ViewCompat.replaceAccessibilityAction(tile, AccessibilityActionCompat.ACTION_CLICK,
                getString(R.string.home_tile_action), null);
        return tile;
    }

    /** Adds one card per swap. There are only a few, so a simple LinearLayout is enough. */
    private void showSwaps(@NonNull List<RuleRunner.ShownSwap> swaps) {
        if (swaps.equals(shownSwaps)) {
            return;
        }
        shownSwaps = new ArrayList<>(swaps);
        swapsList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (RuleRunner.ShownSwap swap : swaps) {
            BatchInfo kit = swap.getKitBatch();
            View card = inflater.inflate(R.layout.item_swap, swapsList, false);
            TextView product = card.findViewById(R.id.swap_product);
            TextView sample = card.findViewById(R.id.swap_sample);
            Button review = card.findViewById(R.id.swap_review);
            product.setText(BatchText.productChoice(this, kit));
            sample.setVisibility(kit.isSample() ? View.VISIBLE : View.GONE);
            SwapDiagram.bind(card.findViewById(R.id.swap_diagram), SwapDiagram.of(this, swap));
            review.setContentDescription(SwapText.buttonDescription(this, swap));
            review.setOnClickListener(view -> startActivity(SwapReviewActivity.newIntent(this,
                    kit.getId(), swap.getPantryBatch().getId(), swap.getQuantity())));
            swapsList.addView(card);
        }
    }
}

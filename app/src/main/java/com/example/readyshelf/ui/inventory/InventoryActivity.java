package com.example.readyshelf.ui.inventory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readyshelf.R;
import com.example.readyshelf.data.StorageLocation;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.InventoryFilter.DateOrder;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.ui.batch.BatchDetailsActivity;
import com.example.readyshelf.ui.batch.BatchEditActivity;
import com.example.readyshelf.ui.common.AddButton;
import com.example.readyshelf.ui.common.BatchAdapter;
import com.example.readyshelf.ui.common.DisplayNames;
import com.example.readyshelf.ui.common.LargeText;
import com.example.readyshelf.ui.common.MessageText;
import com.example.readyshelf.ui.common.MoreText;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.common.StatusTile;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Every batch, with search, location, category, and status filters (chips), and date order (a
 * two-button switch). The choices live in the ViewModel, so they survive a rotation. Home's
 * status tiles open this screen with a status already chosen.
 *
 * <p>Since 2026-10-19 (the student's choice) the filters fold under "Filters", so the list starts
 * near the top. While they are folded, each chosen filter shows as a chip under the search box;
 * a tap on it (or its ✕) sets that filter back.
 *
 * <p>Since 2026-10-22 (the student's choice) the top bar has only a home icon when a status tile
 * opened this screen, instead of the back arrow and "Inventory". A tap on it goes back to Home.
 *
 * <p>Since 2026-10-23 (the student's choice) such a status list also starts with that status's
 * tile as its header, on a pine band under the top bar (StatusTile), in place of the status's chip.
 */
public class InventoryActivity extends AppCompatActivity {

    private static final String EXTRA_STATUS = "com.example.readyshelf.DATE_STATUS";
    // The Status chips' order: the same as Home's tiles (most urgent first), then "Later date".
    // DateStatus lists its values in a different order.
    private static final DateStatus[] STATUS_ORDER = {DateStatus.PAST_DATE, DateStatus.TODAY,
            DateStatus.NEAR_DATE, DateStatus.NEEDS_REVIEW, DateStatus.LATER};

    /** The filters that a chip under the search box can set back. */
    private enum Filter { LOCATION, CATEGORY, STATUS, ORDER, ARCHIVED }

    /** One chosen filter, shown as a chip: which filter it is, and its words. */
    private static final class AppliedFilter {
        final Filter filter;
        final String label;

        AppliedFilter(@NonNull Filter filter, @NonNull String label) {
            this.filter = filter;
            this.label = label;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof AppliedFilter && filter == ((AppliedFilter) other).filter
                    && label.equals(((AppliedFilter) other).label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filter, label);
        }
    }

    /**
     * Opens Inventory showing only the batches with this status. Home's status tiles use it; the
     * household can then choose another status, or "All statuses", like any other filter.
     */
    @NonNull
    public static Intent newStatusIntent(@NonNull Context context, @NonNull DateStatus status) {
        return new Intent(context, InventoryActivity.class).putExtra(EXTRA_STATUS, status.name());
    }

    private InventoryViewModel viewModel;
    private BatchAdapter adapter;
    private ChipGroup locationChips;
    // True while the location chips are made again; their checks then are not the household's.
    private boolean buildingLocationChips;
    private ChipGroup categoryChips;
    private View statusHeading;
    private ChipGroup statusChips;
    private MaterialButtonToggleGroup dateOrder;
    private MaterialSwitch showArchived;
    // The folded filters, and the chips of the chosen ones that show while they are folded.
    private View filtersPanel;
    private ChipGroup appliedFilters;
    // The chosen filters whose chips are on screen; they are made again only when this changes,
    // so TalkBack doesn't lose its place each time the database sends new values.
    @Nullable private List<AppliedFilter> shownApplied;
    private TextView statusMessage;
    private TextView countText;
    private TextView emptyText;
    // True when a status tile on Home opened this screen: the list then has a header (the band).
    private boolean fromStatusTile;
    private View statusBand;
    private View statusTile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_inventory);
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        DateStatus tileStatus = statusFrom(getIntent());
        viewModel.start(tileStatus);
        fromStatusTile = tileStatus != null;
        if (fromStatusTile) {
            showHomeButton();
        }
        statusBand = findViewById(R.id.inventory_status_band);
        statusTile = findViewById(R.id.inventory_status_tile);
        StatusTile.showAsHeader(statusTile);

        statusMessage = findViewById(R.id.inventory_status_message);
        countText = findViewById(R.id.inventory_count);
        emptyText = findViewById(R.id.inventory_empty);

        ExtendedFloatingActionButton addButton = findViewById(R.id.fab_add_batch);
        addButton.setOnClickListener(view -> startActivity(BatchEditActivity.newAddIntent(this)));
        AddButton.setUp(addButton, findViewById(R.id.inventory_scroll));

        EditText search = findViewById(R.id.inventory_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // Not needed.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // Not needed.
            }

            @Override
            public void afterTextChanged(Editable text) {
                viewModel.setSearchText(text.toString());
            }
        });
        // The list already follows the typing, so the keyboard's search key only closes the
        // keyboard, and the results under it come into view.
        search.setOnEditorActionListener((field, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return false;
            }
            InputMethodManager keyboard = getSystemService(InputMethodManager.class);
            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(field.getWindowToken(), 0);
            }
            return true;
        });

        // The filters start folded (also after a Home tile), so the list starts near the top.
        // While they are folded, the chosen ones show as chips (showAppliedFilters).
        filtersPanel = findViewById(R.id.inventory_filters_panel);
        appliedFilters = findViewById(R.id.inventory_applied_filters);
        MoreText.setUp(findViewById(R.id.inventory_filters_toggle), filtersPanel,
                savedInstanceState, R.string.inventory_filters_show,
                R.string.inventory_filters_hide, this::showAppliedFilters);

        setUpCategoryFilter();
        setUpStatusFilter();
        setUpDateOrder();
        showArchived = findViewById(R.id.inventory_show_archived);
        showArchived.setChecked(viewModel.isShowArchived());
        showArchived.setOnCheckedChangeListener(
                (button, checked) -> viewModel.setShowArchived(checked));
        locationChips = findViewById(R.id.inventory_location_chips);
        locationChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!buildingLocationChips && !checkedIds.isEmpty()) {
                viewModel.setLocationId((Long) group.findViewById(checkedIds.get(0)).getTag());
            }
        });
        viewModel.getLocations().observe(this, this::showLocations);

        adapter = new BatchAdapter(
                batch -> startActivity(BatchDetailsActivity.newIntent(this, batch.getId())));
        RecyclerView list = findViewById(R.id.inventory_list);
        ScreenSetup.setUpList(list, adapter);

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
        // Whether the filters are open, so a rotation keeps them as they were.
        MoreText.save(outState, filtersPanel);
    }

    /** "All categories", then one chip per category; the ViewModel's choice is checked. */
    private void setUpCategoryFilter() {
        categoryChips = findViewById(R.id.inventory_category_chips);
        ItemCategory chosen = viewModel.getCategory();
        addChip(categoryChips, getString(R.string.filter_all_categories), null, chosen == null);
        for (ItemCategory category : ItemCategory.values()) {
            addChip(categoryChips, getString(DisplayNames.category(category)), category,
                    category == chosen);
        }
        categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                viewModel.setCategory((ItemCategory) group.findViewById(checkedIds.get(0)).getTag());
            }
        });
    }

    /** The status a Home tile asked for, or null ("All statuses") for any other start. */
    @Nullable
    private static DateStatus statusFrom(@NonNull Intent intent) {
        String name = intent.getStringExtra(EXTRA_STATUS);
        return name == null ? null : DateStatus.valueOf(name);
    }

    /**
     * The top bar after a tap on a status tile (the student's choice on 2026-10-22): only a home
     * icon, light like the back arrow on the pine bar (ReadyShelf.TopBar tints it), with no
     * "Inventory". A tap on it closes this screen, like the back arrow (ScreenSetup). Only Home's
     * status tiles open Inventory this way, so Home is the screen under it. The screen's own label
     * (the manifest) stays "Inventory".
     */
    private void showHomeButton() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(null);
        toolbar.setNavigationIcon(R.drawable.ic_home);
        toolbar.setNavigationContentDescription(R.string.navigate_home);
    }

    /**
     * True when the list shows its status as a header: a status tile opened this screen, the date
     * rules gave the batches their statuses, and a status is chosen (also one chosen later in the
     * filters). "All statuses" has no header.
     */
    private boolean showsStatusHeader(@Nullable InventoryViewModel.State state) {
        return fromStatusTile && state != null && state.statusesKnown && state.status != null;
    }

    /**
     * "All statuses", then one chip per status in the tiles' order; the ViewModel's choice is
     * checked. The words are the same as on the batches' status labels.
     */
    private void setUpStatusFilter() {
        statusHeading = findViewById(R.id.inventory_status_heading);
        statusChips = findViewById(R.id.inventory_status_chips);
        DateStatus chosen = viewModel.getStatus();
        addChip(statusChips, getString(R.string.filter_all_statuses), null, chosen == null);
        for (DateStatus status : STATUS_ORDER) {
            addChip(statusChips, getString(DisplayNames.status(status)), status, status == chosen);
        }
        statusChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                viewModel.setStatus((DateStatus) group.findViewById(checkedIds.get(0)).getTag());
            }
        });
    }

    /** "Soonest first" or "Latest first". (R.id values can't be switch cases.) */
    private void setUpDateOrder() {
        dateOrder = findViewById(R.id.inventory_order);
        // Buttons in this group keep their words on one line, so with large text they go under
        // each other, each the full width, instead of cutting their words off.
        if (LargeText.stackRows(getResources())) {
            dateOrder.setOrientation(LinearLayout.VERTICAL);
            for (int i = 0; i < dateOrder.getChildCount(); i++) {
                View button = dateOrder.getChildAt(i);
                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) button.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.weight = 0;
                button.setLayoutParams(params);
            }
        }
        dateOrder.check(viewModel.getDateOrder() == DateOrder.LATEST_FIRST
                ? R.id.inventory_order_latest : R.id.inventory_order_soonest);
        dateOrder.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setDateOrder(checkedId == R.id.inventory_order_latest
                        ? DateOrder.LATEST_FIRST : DateOrder.SOONEST_FIRST);
            }
        });
    }

    /** Makes the location chips again when locations change, and keeps the current choice. */
    private void showLocations(List<StorageLocation> locations) {
        buildingLocationChips = true;
        locationChips.removeAllViews();
        Long chosenId = viewModel.getLocationId();
        addChip(locationChips, getString(R.string.filter_all_locations), null, chosenId == null);
        for (StorageLocation location : locations) {
            addChip(locationChips, location.name, location.id,
                    chosenId != null && chosenId == location.id);
        }
        buildingLocationChips = false;
        showAppliedFilters(); // the chosen location's chip needs its name
    }

    /** Adds one filter chip. Its tag is what it filters by: null for "All …". */
    private void addChip(@NonNull ChipGroup group, @NonNull String label, @Nullable Object value,
                         boolean checked) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(value);
        group.addView(chip);
        chip.setChecked(checked);
    }

    private void show(InventoryViewModel.State state) {
        // Without the date rules no batch has a status, so the Status chips can't filter.
        int statusFilter = state.statusesKnown ? View.VISIBLE : View.GONE;
        statusHeading.setVisibility(statusFilter);
        statusChips.setVisibility(statusFilter);

        adapter.submitList(state.rows);
        countText.setText(getResources().getQuantityString(R.plurals.inventory_count,
                state.totalCount, state.shownCount, state.totalCount));

        if (state.totalCount == 0) {
            emptyText.setText(R.string.inventory_empty);
            emptyText.setVisibility(View.VISIBLE);
        } else if (state.shownCount == 0) {
            emptyText.setText(R.string.inventory_no_matches);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }

        MessageText.show(statusMessage,
                state.dateRulesMissing ? R.string.inventory_status_note : 0, state.rulesError);
        // The status list's header: the status's tile, with the same count as on Home.
        if (showsStatusHeader(state)) {
            StatusTile.bind(statusTile, state.status, state.statusCount);
            statusBand.setVisibility(View.VISIBLE);
        } else {
            statusBand.setVisibility(View.GONE);
        }
        showAppliedFilters();
    }

    // ----- The chosen filters, as chips under the search box -----

    /**
     * While the filters are folded, one chip for each filter that is not at its "All" choice,
     * plus "Latest first" and "Archived batches shown" when they are on; with two or more,
     * "Clear all" after them. While the filters are open, the chips are hidden, because the
     * chosen choices are in view there. The chips are made again only when the list changed.
     */
    private void showAppliedFilters() {
        List<AppliedFilter> applied = new ArrayList<>();
        Long locationId = viewModel.getLocationId();
        String locationName = locationId == null ? null : locationName(locationId);
        if (locationName != null) {
            applied.add(new AppliedFilter(Filter.LOCATION, locationName));
        }
        ItemCategory category = viewModel.getCategory();
        if (category != null) {
            applied.add(new AppliedFilter(Filter.CATEGORY,
                    getString(DisplayNames.category(category))));
        }
        // Without the date rules the status filter isn't used (and its chips are hidden). A
        // status list's header already says the status, so it gets no chip then.
        InventoryViewModel.State state = viewModel.getState().getValue();
        DateStatus status = viewModel.getStatus();
        if (status != null && state != null && state.statusesKnown
                && !showsStatusHeader(state)) {
            applied.add(new AppliedFilter(Filter.STATUS, getString(DisplayNames.status(status))));
        }
        if (viewModel.getDateOrder() == DateOrder.LATEST_FIRST) {
            applied.add(new AppliedFilter(Filter.ORDER, getString(R.string.order_latest_first)));
        }
        if (viewModel.isShowArchived()) {
            applied.add(new AppliedFilter(Filter.ARCHIVED,
                    getString(R.string.filter_archived_shown)));
        }

        boolean folded = filtersPanel.getVisibility() != View.VISIBLE;
        appliedFilters.setVisibility(folded && !applied.isEmpty() ? View.VISIBLE : View.GONE);
        if (applied.equals(shownApplied)) {
            return;
        }
        shownApplied = applied;
        appliedFilters.removeAllViews();
        for (AppliedFilter filter : applied) {
            addAppliedChip(filter.label, getString(R.string.filter_chip_description, filter.label),
                    true, () -> setBack(filter.filter));
        }
        if (applied.size() >= 2) {
            addAppliedChip(getString(R.string.filter_clear_all), null, false, this::setAllBack);
        }
    }

    /**
     * One chip under the search box. A tap on it (or on its ✕) calls {@code onTap}, after the
     * tap itself is finished: setting a filter back makes the chips again, this one too.
     */
    private void addAppliedChip(@NonNull String text, @Nullable String spokenLabel,
                                boolean withRemoveIcon, @NonNull Runnable onTap) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_applied_filter_chip,
                appliedFilters, false);
        chip.setText(text);
        chip.setContentDescription(spokenLabel);
        chip.setCloseIconVisible(withRemoveIcon);
        View.OnClickListener tap = view -> appliedFilters.post(onTap);
        chip.setOnClickListener(tap);
        if (withRemoveIcon) {
            chip.setOnCloseIconClickListener(tap);
            // TalkBack says "Double-tap to remove this filter" instead of "to activate".
            ViewCompat.replaceAccessibilityAction(chip, AccessibilityActionCompat.ACTION_CLICK,
                    getString(R.string.filter_chip_action), null);
        }
        appliedFilters.addView(chip);
    }

    /**
     * Sets one filter back to its "All" choice (or "Soonest first", or archived batches hidden)
     * by choosing that in the folded filters, so their own listeners tell the ViewModel.
     */
    private void setBack(@NonNull Filter filter) {
        if (filter == Filter.LOCATION) {
            checkChip(locationChips, null);
        } else if (filter == Filter.CATEGORY) {
            checkChip(categoryChips, null);
        } else if (filter == Filter.STATUS) {
            checkChip(statusChips, null);
        } else if (filter == Filter.ORDER) {
            dateOrder.check(R.id.inventory_order_soonest);
        } else {
            showArchived.setChecked(false);
        }
    }

    /**
     * "Clear all": sets back the filters whose chips are shown. A status list's header has no
     * chip, so its status stays.
     */
    private void setAllBack() {
        if (shownApplied == null) {
            return;
        }
        for (AppliedFilter applied : new ArrayList<>(shownApplied)) {
            setBack(applied.filter);
        }
    }

    /** Checks the chip that stands for {@code value} (null is the "All …" chip). */
    private static void checkChip(@NonNull ChipGroup group, @Nullable Object value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View chip = group.getChildAt(i);
            if (Objects.equals(chip.getTag(), value)) {
                group.check(chip.getId());
                return;
            }
        }
    }

    /** The name of a location, or null before the locations have loaded. */
    @Nullable
    private String locationName(long id) {
        List<StorageLocation> locations = viewModel.getLocations().getValue();
        if (locations != null) {
            for (StorageLocation location : locations) {
                if (location.id == id) {
                    return location.name;
                }
            }
        }
        return null;
    }
}

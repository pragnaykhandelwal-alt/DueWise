package com.example.readyshelf.ui.inventory;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.data.StorageLocation;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateStatus;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.InventoryFilter;
import com.example.readyshelf.domain.InventoryFilter.DateOrder;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.RuleRunner;
import com.example.readyshelf.ui.common.BatchRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Prepares the Inventory list. It keeps the search text and filter choices,
 * so they survive screen rotation.
 */
public class InventoryViewModel extends AndroidViewModel {

    private static final String TAG = "InventoryViewModel";

    /** Everything the list part of the screen shows. */
    public static final class State {
        @NonNull public List<BatchRow> rows = new ArrayList<>();
        public int shownCount;
        public int totalCount;
        public boolean dateRulesMissing;
        @Nullable public String rulesError;
        /**
         * True when the date rules gave the batches their statuses. Only then can the Status
         * chips filter; otherwise they are hidden and every batch is listed.
         */
        public boolean statusesKnown;
        /** The chosen status (null means "All statuses"), the one {@link #statusCount} counts. */
        @Nullable public DateStatus status;
        /**
         * How many batches that are not archived have the chosen status, with no other filter:
         * the same number as that status's tile on Home. 0 without a chosen status.
         */
        public int statusCount;
    }

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final LiveData<List<BatchInfo>> batches;
    private final LiveData<DateThresholds> thresholds;
    private final LiveData<List<StorageLocation>> locations;
    private final MutableLiveData<LocalDate> today = new MutableLiveData<>(LocalDate.now());
    private final MediatorLiveData<State> state = new MediatorLiveData<>();

    // True after start(), so the status a Home tile asked for is chosen only once.
    private boolean started;

    // The household's current choices.
    private String searchText = "";
    @Nullable private Long locationId;          // null means "All locations"
    @Nullable private ItemCategory category;    // null means "All categories"
    @Nullable private DateStatus status;        // null means "All statuses"
    private DateOrder dateOrder = DateOrder.SOONEST_FIRST;
    private boolean showArchived;

    public InventoryViewModel(@NonNull Application application) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        batches = data.repository.observeBatchesWithArchived();
        thresholds = data.settings.observeThresholds();
        locations = data.repository.observeLocations();
        state.addSource(batches, value -> update());
        state.addSource(thresholds, value -> update());
        state.addSource(today, value -> update());
    }

    /**
     * Chooses the status that a Home tile asked for (null means "All statuses"). Only the first
     * call counts, so after a rotation the household's own later choice stays.
     */
    public void start(@Nullable DateStatus initialStatus) {
        if (started) {
            return;
        }
        started = true;
        status = initialStatus;
        update();
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    @NonNull
    public LiveData<List<StorageLocation>> getLocations() {
        return locations;
    }

    @Nullable
    public Long getLocationId() {
        return locationId;
    }

    @Nullable
    public ItemCategory getCategory() {
        return category;
    }

    @Nullable
    public DateStatus getStatus() {
        return status;
    }

    @NonNull
    public DateOrder getDateOrder() {
        return dateOrder;
    }

    /** True when archived batches are shown too. */
    public boolean isShowArchived() {
        return showArchived;
    }

    // Each setter ignores a choice that didn't change, so the list isn't rebuilt for nothing.

    public void setSearchText(@NonNull String text) {
        if (!text.equals(searchText)) {
            searchText = text;
            update();
        }
    }

    public void setLocationId(@Nullable Long newLocationId) {
        if (!Objects.equals(newLocationId, locationId)) {
            locationId = newLocationId;
            update();
        }
    }

    public void setCategory(@Nullable ItemCategory newCategory) {
        if (newCategory != category) {
            category = newCategory;
            update();
        }
    }

    public void setStatus(@Nullable DateStatus newStatus) {
        if (newStatus != status) {
            status = newStatus;
            update();
        }
    }

    public void setDateOrder(@NonNull DateOrder newOrder) {
        if (newOrder != dateOrder) {
            dateOrder = newOrder;
            update();
        }
    }

    public void setShowArchived(boolean show) {
        if (show != showArchived) {
            showArchived = show;
            update();
        }
    }

    /** The Activity calls this when it comes back to the front, in case the date changed. */
    public void refreshToday() {
        LocalDate now = LocalDate.now();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
    }

    private void update() {
        List<BatchInfo> allBatches = batches.getValue();
        DateThresholds currentThresholds = thresholds.getValue();
        LocalDate currentDay = today.getValue();
        if (allBatches == null || currentThresholds == null || currentDay == null) {
            return; // still loading
        }

        // Archived batches are left out unless "Show archived batches" is on.
        List<BatchInfo> pool = new ArrayList<>();
        for (BatchInfo batch : allBatches) {
            if (showArchived || !batch.isArchived()) {
                pool.add(batch);
            }
        }
        List<BatchInfo> shown = InventoryFilter.apply(pool, searchText, locationId, category, dateOrder);
        State next = new State();
        next.totalCount = pool.size();
        next.shownCount = shown.size();
        next.status = status;

        DatePolicyContract policy = app.datePolicy;
        if (policy == null) {
            next.rows = BatchRow.withoutStatus(shown);
            next.dateRulesMissing = true;
        } else {
            try {
                // Archived batches (shown when "Show archived batches" is on) are never given
                // to the date rules, so their rows have no status, and a chosen status leaves
                // them out. Every batch gets its status, not only the shown ones, so the chosen
                // status's count is the same as its tile's on Home (the status list's header).
                Map<Long, DateStatus> statuses = RuleRunner.statusesOfActive(allBatches, policy,
                        currentDay, currentThresholds);
                if (status != null) {
                    next.statusCount = Collections.frequency(statuses.values(), status);
                }
                List<BatchInfo> withStatus = InventoryFilter.withStatus(shown, statuses, status);
                List<BatchRow> rows = new ArrayList<>();
                for (BatchInfo batch : withStatus) {
                    rows.add(new BatchRow(batch, statuses.get(batch.getId())));
                }
                next.rows = rows;
                next.shownCount = withStatus.size();
                next.statusesKnown = true;
            } catch (RuntimeException error) {
                // A mistake in the rules shows as text instead of closing the app.
                Log.e(TAG, "The date rules threw an error", error);
                next.rows = BatchRow.withoutStatus(shown);
                next.rulesError = error.toString();
            }
        }
        state.setValue(next);
    }
}

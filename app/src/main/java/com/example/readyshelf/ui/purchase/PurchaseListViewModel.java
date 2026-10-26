package com.example.readyshelf.ui.purchase;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.data.PurchaseItemInfo;
import com.example.readyshelf.data.PurchaseListStore;
import com.example.readyshelf.data.ReadyShelfRepository.PurchaseListener;
import com.example.readyshelf.data.ReadyShelfRepository.SaveListener;
import com.example.readyshelf.data.ReadyShelfRepository.SaveResult;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.BatchWithStatus;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.PlanReason;
import com.example.readyshelf.domain.PurchaseSuggestions;
import com.example.readyshelf.domain.RenewalList;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.domain.RuleRunner;
import com.example.readyshelf.domain.StockChange;
import com.example.readyshelf.ui.common.BatchRow;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.DisplayNames;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Prepares the Purchase/Renewal list ("Purchase list" until 2026-10-24): the items the household
 * plans to buy, the replacement needs (kit items the household buys again that the swaps don't
 * cover, and expired ones in any place), and the Renewal section (documents and subscriptions
 * dated today or earlier; see RenewalList). Each need can add its product and place to the list.
 * Each Renewal item can be saved as renewed with a new date, or discarded and archived. Until
 * 2026-10-22 the totals were a "Suggestions" section of their own; the student removed it.
 *
 * <p>Needs and Renewal items are made again from the current stock whenever the batches, the
 * Settings numbers, or the date change, so their amounts are always recalculated. Nothing about
 * them is stored. Items on the list are stored, but only as notes: they never change stock.
 */
public class PurchaseListViewModel extends AndroidViewModel {

    private static final String TAG = "PurchaseListViewModel";

    /** One item on the list, with what the needs of its key add up to right now. Never changes. */
    public static final class EntryRow {
        @NonNull public final PurchaseItemInfo item;
        /** True when the needs could be made (the swap rules are connected and worked). */
        public final boolean needsKnown;
        /** What the needs add up to now for the same product and place, or null when none. */
        @Nullable public final Integer neededNow;

        EntryRow(@NonNull PurchaseItemInfo item, boolean needsKnown, @Nullable Integer neededNow) {
            this.item = item;
            this.needsKnown = needsKnown;
            this.neededNow = neededNow;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EntryRow)) {
                return false;
            }
            EntryRow that = (EntryRow) other;
            return needsKnown == that.needsKnown && item.equals(that.item)
                    && Objects.equals(neededNow, that.neededNow);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, needsKnown, neededNow);
        }
    }

    /** Everything the Purchase/Renewal list screen shows. */
    public static final class State {
        @NonNull public List<EntryRow> entries = new ArrayList<>();
        @StringRes public int entriesMessage;      // 0 means "no message"
        // One card per need, with "Add to purchase list" or why it can't be added.
        @NonNull public List<BatchRow> needRows = new ArrayList<>();
        @StringRes public int needsMessage;        // 0 means "no message"
        @Nullable public String rulesError;        // set when the swap rules threw an error
        // One card per Renewal item, with "Renewed" and "Discard" (since 2026-10-24).
        @NonNull public List<BatchRow> renewalRows = new ArrayList<>();
        @StringRes public int renewalMessage;      // 0 means "no message"
        @Nullable public String renewalError;      // set when the date rules threw an error
        @Nullable public String result;            // why the last change was not made
        public boolean busy;                       // a change is being saved
    }

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final LiveData<List<BatchInfo>> batches;
    private final LiveData<DateThresholds> thresholds;
    private final LiveData<List<PurchaseItemInfo>> items;
    private final MutableLiveData<LocalDate> today = new MutableLiveData<>(LocalDate.now());
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final Executor mainThread;

    private boolean busy;
    @Nullable private String result;
    // What the needs add up to for each product and place, from the last update.
    @NonNull private Map<PurchaseSuggestions.Key, PurchaseSuggestions.Suggestion> totals =
            Collections.emptyMap();
    // A saved list change, shown once as a short message at the bottom (Confirmations).
    private final MutableLiveData<String> savedMessage = new MutableLiveData<>();

    public PurchaseListViewModel(@NonNull Application application) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        batches = data.repository.observeBatches();
        thresholds = data.settings.observeThresholds();
        items = data.repository.observePurchaseItems();
        mainThread = ContextCompat.getMainExecutor(application);
        state.addSource(batches, value -> update());
        state.addSource(thresholds, value -> update());
        state.addSource(today, value -> update());
        state.addSource(items, value -> update());
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    /** What a saved list change did, or null. The screen shows it once. */
    @NonNull
    public LiveData<String> getSavedMessage() {
        return savedMessage;
    }

    /** The screen showed the message, so it isn't shown again after the screen rotates. */
    public void savedMessageShown() {
        savedMessage.setValue(null);
    }

    /** The Activity calls this when it comes back to the front, in case the date changed. */
    public void refreshToday() {
        LocalDate now = LocalDate.now();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
    }

    /**
     * What the needs add up to for this batch's product and place, as the screen shows them now,
     * or null when there are none. "Add to purchase list" adds this amount.
     */
    @Nullable
    public PurchaseSuggestions.Suggestion totalFor(@NonNull BatchInfo batch) {
        return totals.get(keyOf(batch));
    }

    // ----- Changes the household confirmed in a dialog -----

    /** Adds a product and place to the list, with the amount the dialog showed. */
    public void add(@NonNull PurchaseSuggestions.Suggestion suggestion) {
        if (busy || suggestion.isSample()) {
            return;
        }
        BatchInfo batch = suggestion.getFirstBatch();
        String amount = BatchText.amount(getApplication(), suggestion.getQuantity(), batch.getUnit());
        String product = BatchText.productChoice(getApplication(), batch);
        String kit = batch.getLocationName();
        busy = true;
        data.repository.addToPurchaseList(suggestion.getKey().getProductId(),
                suggestion.getKey().getKitLocationId(), suggestion.getQuantity(),
                listener(R.string.purchase_added, amount, product, kit));
        update();
    }

    /** Changes an item's amount to the amount suggested now. */
    public void changeQuantity(@NonNull EntryRow row, int newQuantity) {
        if (busy) {
            return;
        }
        PurchaseItemInfo item = row.item;
        String product = productText(item);
        busy = true;
        data.repository.changePurchaseQuantity(item.getId(), item.getQuantity(), newQuantity,
                listener(R.string.purchase_changed, product, item.getLocationName(),
                        BatchText.amount(getApplication(), newQuantity, item.getUnit())));
        update();
    }

    /** Removes an item from the list. */
    public void remove(@NonNull EntryRow row) {
        if (busy) {
            return;
        }
        PurchaseItemInfo item = row.item;
        busy = true;
        data.repository.removeFromPurchaseList(item.getId(), listener(R.string.purchase_removed,
                productText(item), item.getLocationName()));
        update();
    }

    /**
     * "Renewed": the batch gets {@code newDate} as its confirmed date, with the same label type.
     * Does nothing unless the new date is after today (the date picker offers only those).
     */
    public void renew(@NonNull BatchInfo batch, @NonNull LocalDate newDate) {
        if (busy || !newDate.isAfter(LocalDate.now())) {
            return;
        }
        Context context = getApplication();
        String newDateText = BatchText.labeledDate(context, newDate, batch.getDateLabel());
        busy = true;
        data.repository.renewBatch(batch.getId(), batch.getQuantity(), batch.getDate(), newDate,
                context.getString(R.string.history_renewed, BatchText.dateEntry(context, batch),
                        newDateText),
                batchListener(R.string.renewal_saved_renewed, batch.getName(), newDateText));
        update();
    }

    /** "Discard": records the whole batch as discarded, then archives it. */
    public void discard(@NonNull BatchInfo batch) {
        if (busy) {
            return;
        }
        Context context = getApplication();
        String all = BatchText.amount(context, batch.getQuantity(), batch.getUnit());
        String none = BatchText.amount(context, 0, batch.getUnit());
        busy = true;
        data.repository.discardAndArchive(batch.getId(), batch.getQuantity(),
                context.getString(R.string.history_discarded, all, all, none),
                context.getString(R.string.history_renewal_archived),
                batchListener(R.string.renewal_saved_discarded, batch.getName()));
        update();
    }

    /** "Canned beans — Low sodium · 15 oz · unit: cans" for an item on the list. */
    @NonNull
    public String productText(@NonNull PurchaseItemInfo item) {
        return BatchText.productChoice(getApplication(), item.getName(), item.getVariant(),
                item.getPackageSize(), item.getUnit());
    }

    /**
     * Shows how a change ended. {@code savedText} and its values describe a saved change, which
     * shows once as a short message; a change that was not made stays explained on the screen.
     */
    @NonNull
    private PurchaseListener listener(@StringRes int savedText, @NonNull Object... values) {
        return answer -> mainThread.execute(() -> {
            busy = false;
            Context context = getApplication();
            if (answer == PurchaseListStore.Result.SAVED) {
                result = null;
                savedMessage.setValue(context.getString(savedText, values));
            } else if (answer == PurchaseListStore.Result.ALREADY_ON_LIST) {
                result = context.getString(R.string.purchase_already_on_list);
            } else if (answer == PurchaseListStore.Result.ITEM_CHANGED) {
                result = context.getString(R.string.purchase_item_changed);
            } else {
                result = context.getString(R.string.purchase_change_failed);
            }
            update();
        });
    }

    /** The same as {@link #listener}, for a change to a batch: "Renewed" or "Discard". */
    @NonNull
    private SaveListener batchListener(@StringRes int savedText, @NonNull Object... values) {
        return answer -> mainThread.execute(() -> {
            busy = false;
            Context context = getApplication();
            if (answer == SaveResult.SAVED) {
                result = null;
                savedMessage.setValue(context.getString(savedText, values));
            } else if (answer == SaveResult.BATCH_CHANGED) {
                result = context.getString(R.string.purchase_item_changed);
            } else {
                result = context.getString(R.string.purchase_change_failed);
            }
            update();
        });
    }

    // ----- What the screen shows -----

    private void update() {
        List<BatchInfo> allBatches = batches.getValue();
        DateThresholds currentThresholds = thresholds.getValue();
        LocalDate currentDay = today.getValue();
        List<PurchaseItemInfo> listed = items.getValue();
        if (allBatches == null || currentThresholds == null || currentDay == null || listed == null) {
            return; // still loading
        }

        State next = new State();
        next.busy = busy;
        next.result = result;
        Map<PurchaseSuggestions.Key, Integer> listedQuantities = new HashMap<>();
        for (PurchaseItemInfo item : listed) {
            listedQuantities.put(keyOf(item), item.getQuantity());
        }

        // Null when the needs couldn't be made (the swap rules are missing or threw an error).
        Map<PurchaseSuggestions.Key, PurchaseSuggestions.Suggestion> nextTotals = null;
        RotationPlannerContract planner = app.rotationPlanner;
        if (planner == null) {
            next.needsMessage = R.string.rules_not_ready_purchase;
        } else {
            try {
                Context context = getApplication();
                List<RuleRunner.ShownNeed> needs =
                        RuleRunner.plan(allBatches, planner, currentDay, currentThresholds).getNeeds();
                nextTotals = new HashMap<>();
                for (PurchaseSuggestions.Suggestion total : PurchaseSuggestions.from(needs)) {
                    nextTotals.put(total.getKey(), total);
                }
                for (RuleRunner.ShownNeed need : needs) {
                    next.needRows.add(needRow(context, need, nextTotals, listedQuantities));
                }
                if (next.needRows.isEmpty()) {
                    next.needsMessage = R.string.purchase_empty;
                }
            } catch (RuntimeException error) {
                // A mistake in the rules shows as text instead of closing the app.
                Log.e(TAG, "The swap rules threw an error", error);
                next.rulesError = error.toString();
                nextTotals = null;
            }
        }
        totals = nextTotals == null ? Collections.emptyMap() : nextTotals;

        // Renewal: the statuses come from the date rules, like Home's tiles.
        DatePolicyContract policy = app.datePolicy;
        if (policy == null) {
            next.renewalMessage = R.string.rules_not_ready_date;
        } else {
            try {
                Context context = getApplication();
                for (BatchWithStatus item : RenewalList.of(RuleRunner.needsReview(allBatches,
                        policy, currentDay, currentThresholds))) {
                    next.renewalRows.add(renewalRow(context, item));
                }
                if (next.renewalRows.isEmpty()) {
                    next.renewalMessage = R.string.renewal_empty;
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "The date rules threw an error", error);
                next.renewalError = error.toString();
            }
        }

        for (PurchaseItemInfo item : listed) {
            PurchaseSuggestions.Suggestion total = totals.get(keyOf(item));
            next.entries.add(new EntryRow(item, nextTotals != null,
                    total == null ? null : total.getQuantity()));
        }
        if (next.entries.isEmpty()) {
            next.entriesMessage = R.string.purchase_list_empty;
        }
        state.setValue(next);
    }

    /**
     * One need's card: "Still needs replacement: 2 cans." with the plan's reason, then
     * "Add to purchase list" for its product and place, or a line that says why it can't be
     * added: it is already on the list, or it is made-up sample data.
     */
    @NonNull
    private BatchRow needRow(@NonNull Context context, @NonNull RuleRunner.ShownNeed need,
                             @NonNull Map<PurchaseSuggestions.Key, PurchaseSuggestions.Suggestion> byKey,
                             @NonNull Map<PurchaseSuggestions.Key, Integer> listedQuantities) {
        BatchInfo batch = need.getBatch();
        // A kit batch dated today or earlier also says "No pantry use is recommended" (it is never
        // swapped into the pantry). That doesn't fit a batch outside a kit, so it gets only the
        // date sentence.
        int reason = DisplayNames.planReason(need.getReason());
        if (need.getReason() == PlanReason.DATE_TODAY_OR_PASSED
                && !StockChange.isKit(batch.getLocationType())) {
            reason = R.string.plan_reason_date_today_or_passed_outside_kit;
        }
        // "Still needs replacement: 2 cans. Pantry batches cover only part of it."
        String note = context.getString(R.string.purchase_still_needed,
                BatchText.amount(context, need.getStillNeeded(), batch.getUnit()),
                context.getString(reason));
        PurchaseSuggestions.Suggestion total = byKey.get(keyOf(batch));
        BatchRow.Action action = null;
        if (batch.isSample()) {
            note += "\n" + context.getString(R.string.purchase_need_sample);
        } else if (total != null && PurchaseSuggestions.stateOf(total, listedQuantities)
                == PurchaseSuggestions.ListState.NOT_ON_LIST) {
            action = new BatchRow.Action(context.getString(R.string.purchase_need_add),
                    context.getString(R.string.purchase_need_add_description, batch.getName(),
                            batch.getLocationName()),
                    !busy);
        } else if (total != null) {
            note += "\n" + context.getString(R.string.purchase_need_on_list);
        }
        return new BatchRow(batch, null, note, action);
    }

    /** One Renewal card: the batch with its status, then "Renewed" and "Discard". */
    @NonNull
    private BatchRow renewalRow(@NonNull Context context, @NonNull BatchWithStatus item) {
        BatchInfo batch = item.getBatch();
        BatchRow.Action renewed = new BatchRow.Action(context.getString(R.string.renewal_renewed),
                context.getString(R.string.renewal_renewed_description, batch.getName(),
                        batch.getLocationName()),
                !busy);
        BatchRow.Action discard = new BatchRow.Action(context.getString(R.string.renewal_discard),
                context.getString(R.string.renewal_discard_description, batch.getName(),
                        batch.getLocationName()),
                !busy);
        return new BatchRow(batch, item.getStatus(), null, renewed, discard);
    }

    @NonNull
    private static PurchaseSuggestions.Key keyOf(@NonNull PurchaseItemInfo item) {
        return new PurchaseSuggestions.Key(item.getProductId(), item.getKitLocationId());
    }

    @NonNull
    private static PurchaseSuggestions.Key keyOf(@NonNull BatchInfo batch) {
        return new PurchaseSuggestions.Key(batch.getProductId(), batch.getLocationId());
    }
}

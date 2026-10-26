package com.example.readyshelf.ui.swap;

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
import androidx.lifecycle.SavedStateHandle;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.data.ReadyShelfRepository.SwapListener;
import com.example.readyshelf.data.SwapAction;
import com.example.readyshelf.data.SwapStore;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateThresholds;
import com.example.readyshelf.domain.RotationPlannerContract;
import com.example.readyshelf.domain.RuleRunner;
import com.example.readyshelf.domain.StockChange;
import com.example.readyshelf.domain.SwapPreview;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.SwapDiagram;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Prepares the Review swap screen and saves the swap when the household confirms it.
 *
 * <p>The swap is found once, in a fresh plan, and then kept ("frozen") with every text the
 * screen shows, so "Confirm swap" saves exactly what the household read. If a later plan no
 * longer contains it, the screen says so and hides "Confirm swap". The save checks everything
 * again in one database transaction anyway (see SwapStore).
 *
 * <p>The action ID is made once and kept in the SavedStateHandle, so a second tap, a rotation,
 * or Android closing and reopening the app can't save the same swap twice.
 */
public class SwapReviewViewModel extends AndroidViewModel {

    private static final String TAG = "SwapReviewViewModel";
    private static final String KEY_ACTION_ID = "actionId";

    /** The texts of the frozen swap. They don't change while the screen is open. */
    public static final class Texts {
        @NonNull public String name = "";
        @NonNull public String productDetails = "";
        public boolean sample;
        @NonNull public String eachWay = "";
        // The swap picture: both moves, read aloud as the two full sentences.
        @Nullable public SwapDiagram.Content diagram;
        @NonNull public String whyRules = "";
        @NonNull public String facts = "";
        // The "After the swap" table: the product's total in each place, before and after.
        @NonNull public String kitPlace = "";
        @NonNull public String kitBefore = "";
        @NonNull public String kitAfter = "";
        @NonNull public String pantryPlace = "";
        @NonNull public String pantryBefore = "";
        @NonNull public String pantryAfter = "";
        // What TalkBack reads for the table: "Granola bars in Earthquake kit: 6 bars before,
        // 6 bars after." for each place.
        @NonNull public String totalsSpoken = "";
        @NonNull public String afterKit = "";
        @NonNull public String afterPantry = "";
        public boolean partMoves;
    }

    /** Everything the screen shows. */
    public static final class State {
        @Nullable public Texts texts;           // null until the swap is found
        @StringRes public int message;          // "not available", "no longer proposed"; 0 = none
        @Nullable public String rulesError;     // set when the swap rules threw an error
        public boolean canConfirm;
        public boolean saving;
        @StringRes public int result;           // why the last confirm saved nothing; 0 = none
        public boolean done;                    // the swap is recorded, so the screen closes
    }

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final SavedStateHandle saved;
    private final LiveData<List<BatchInfo>> batches;
    private final LiveData<DateThresholds> thresholds;
    private final MutableLiveData<LocalDate> today = new MutableLiveData<>(LocalDate.now());
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final Executor mainThread;
    @NonNull private final String actionId;
    private final LiveData<SwapAction> receipt;

    private boolean started;
    private long kitBatchId;
    private long pantryBatchId;
    private int quantity;
    @Nullable private RuleRunner.ShownSwap frozen;
    @Nullable private Texts frozenTexts;
    private boolean stale;
    private boolean saving;
    @StringRes private int result;
    private boolean done;

    public SwapReviewViewModel(@NonNull Application application, @NonNull SavedStateHandle savedState) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        saved = savedState;
        String id = saved.get(KEY_ACTION_ID);
        if (id == null) {
            id = UUID.randomUUID().toString();
            saved.set(KEY_ACTION_ID, id);
        }
        actionId = id;
        batches = data.repository.observeBatches();
        thresholds = data.settings.observeThresholds();
        receipt = data.repository.observeSwapReceipt(actionId);
        mainThread = ContextCompat.getMainExecutor(application);
    }

    /** The Activity calls this in onCreate with the swap from Home. Later calls do nothing. */
    public void start(long kitId, long pantryId, int amount) {
        if (started) {
            return;
        }
        started = true;
        kitBatchId = kitId;
        pantryBatchId = pantryId;
        quantity = amount;
        state.addSource(batches, value -> update());
        state.addSource(thresholds, value -> update());
        state.addSource(today, value -> update());
        state.addSource(receipt, value -> update());
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    /** The Activity calls this when it comes back to the front, in case the date changed. */
    public void refreshToday() {
        LocalDate now = LocalDate.now();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
    }

    /** Saves the frozen swap. The screen closes when it is recorded. */
    public void confirm() {
        RotationPlannerContract planner = app.rotationPlanner;
        if (frozen == null || stale || saving || done || planner == null) {
            return;
        }
        SwapStore.Request request = request(frozen);
        SwapListener listener = answer -> mainThread.execute(() -> saveFinished(answer));
        saving = true;
        result = 0;
        data.repository.confirmSwap(request, planner, LocalDate::now, data.settings::current, listener);
        update();
    }

    private void saveFinished(@NonNull SwapStore.Result answer) {
        saving = false;
        if (answer == SwapStore.Result.SAVED || answer == SwapStore.Result.ALREADY_SAVED) {
            done = true;
        } else if (answer == SwapStore.Result.BATCH_CHANGED) {
            result = R.string.swap_error_batch_changed;
            stale = true;
        } else if (answer == SwapStore.Result.NO_LONGER_PROPOSED) {
            result = R.string.swap_error_not_proposed;
            stale = true;
        } else {
            // Nothing was saved. Trying again is harmless: it uses the same action ID.
            result = R.string.swap_error_failed;
        }
        refreshToday();
        update();
    }

    private void update() {
        State next = new State();
        next.saving = saving;
        next.result = result;
        if (done || receipt.getValue() != null) {
            // The receipt is there: this action ID already recorded the swap.
            done = true;
            next.done = true;
            state.setValue(next);
            return;
        }
        RotationPlannerContract planner = app.rotationPlanner;
        if (planner == null) {
            next.message = R.string.rules_not_ready_swaps;
            state.setValue(next);
            return;
        }
        List<BatchInfo> allBatches = batches.getValue();
        DateThresholds numbers = thresholds.getValue();
        LocalDate day = today.getValue();
        if (allBatches == null || numbers == null || day == null) {
            return; // still loading
        }
        try {
            List<RuleRunner.ShownSwap> swaps =
                    RuleRunner.plan(allBatches, planner, day, numbers).getSwaps();
            if (frozen == null && !stale) {
                frozen = find(swaps);
                if (frozen != null) {
                    frozenTexts = texts(frozen, allBatches, day, numbers);
                } else {
                    stale = true;
                }
            } else if (frozen != null && !swaps.contains(frozen)) {
                stale = true;
            }
        } catch (RuntimeException error) {
            // A mistake in the rules shows as text instead of closing the app.
            Log.e(TAG, "The swap rules threw an error", error);
            next.rulesError = error.toString();
        }
        next.texts = frozenTexts;
        if (stale && result == 0) {
            next.message = R.string.swap_stale;
        }
        next.canConfirm = frozen != null && !stale && next.rulesError == null;
        state.setValue(next);
    }

    /** The swap from Home in a fresh plan: the same two batches and the same amount. */
    @Nullable
    private RuleRunner.ShownSwap find(@NonNull List<RuleRunner.ShownSwap> swaps) {
        for (RuleRunner.ShownSwap swap : swaps) {
            if (swap.getKitBatch().getId() == kitBatchId
                    && swap.getPantryBatch().getId() == pantryBatchId
                    && swap.getQuantity() == quantity) {
                return swap;
            }
        }
        return null;
    }

    // ----- Texts -----

    @NonNull
    private Texts texts(@NonNull RuleRunner.ShownSwap swap, @NonNull List<BatchInfo> allBatches,
                        @NonNull LocalDate day, @NonNull DateThresholds numbers) {
        Context context = getApplication();
        BatchInfo kit = swap.getKitBatch();
        BatchInfo pantry = swap.getPantryBatch();
        SwapPreview preview = SwapPreview.of(swap, allBatches);
        String moving = amount(swap.getQuantity(), kit);
        String kitDate = BatchText.dateEntry(context, kit);
        String pantryDate = BatchText.dateEntry(context, pantry);

        Texts texts = new Texts();
        texts.name = kit.getName();
        texts.productDetails = BatchText.productChoiceDetails(context, kit.getVariant(),
                kit.getPackageSize(), kit.getUnit());
        texts.sample = kit.isSample();
        texts.eachWay = context.getString(R.string.swap_each_way, moving);
        texts.diagram = SwapDiagram.of(context, swap);
        texts.whyRules = context.getString(R.string.swap_why_rules,
                dayCount(numbers.getNearDateDays()), dayCount(numbers.getReplacementDays()));

        List<String> facts = new ArrayList<>();
        facts.add(context.getString(R.string.swap_fact_today, BatchText.date(day)));
        facts.add(context.getString(R.string.swap_fact_product, BatchText.productChoice(context, kit)));
        facts.add(context.getString(R.string.swap_fact_kit, kitDate, kit.getLocationName(),
                records(kit)));
        facts.add(context.getString(R.string.swap_fact_pantry, pantryDate, pantry.getLocationName(),
                records(pantry)));
        texts.facts = bullets(facts);

        List<String> inKit = new ArrayList<>();
        inKit.add(preview.getKitBatchLeft() > 0
                ? context.getString(R.string.swap_after_stays, amount(preview.getKitBatchLeft(), kit), kitDate)
                : context.getString(R.string.swap_after_none_stays));
        inKit.add(context.getString(R.string.swap_after_leaves_for_review, pantry.getLocationName(),
                moving, kitDate));
        inKit.add(context.getString(R.string.swap_after_arrives, pantry.getLocationName(), moving,
                pantryDate));
        inKit.add(total(kit, kit.getLocationName(), preview.getKitTotal()));
        texts.afterKit = context.getString(R.string.swap_after_in, kit.getLocationName())
                + "\n" + bullets(inKit);

        List<String> inPantry = new ArrayList<>();
        inPantry.add(preview.getPantryBatchLeft() > 0
                ? context.getString(R.string.swap_after_stays,
                amount(preview.getPantryBatchLeft(), pantry), pantryDate)
                : context.getString(R.string.swap_after_none_stays));
        inPantry.add(context.getString(R.string.swap_after_leaves, kit.getLocationName(), moving,
                pantryDate));
        inPantry.add(context.getString(R.string.swap_after_arrives_for_review, kit.getLocationName(),
                moving, kitDate));
        inPantry.add(total(pantry, pantry.getLocationName(), preview.getPantryTotal()));
        texts.afterPantry = context.getString(R.string.swap_after_in, pantry.getLocationName())
                + "\n" + bullets(inPantry);

        texts.kitPlace = kit.getLocationName();
        texts.kitBefore = amount(preview.getKitTotal().getBefore(), kit);
        texts.kitAfter = amount(preview.getKitTotal().getAfter(), kit);
        texts.pantryPlace = pantry.getLocationName();
        texts.pantryBefore = amount(preview.getPantryTotal().getBefore(), pantry);
        texts.pantryAfter = amount(preview.getPantryTotal().getAfter(), pantry);
        texts.totalsSpoken = total(kit, kit.getLocationName(), preview.getKitTotal()) + " "
                + total(pantry, pantry.getLocationName(), preview.getPantryTotal());

        texts.partMoves = preview.isKitBatchSplit() || preview.isPantryBatchSplit();
        return texts;
    }

    /**
     * "Date and label type confirmed." Since 2026-10-22 the swap rules use no food class or
     * condition answers, so the review doesn't list them.
     */
    @NonNull
    private String records(@NonNull BatchInfo batch) {
        return getApplication().getString(batch.isDateConfirmed()
                ? R.string.swap_fact_confirmed : R.string.swap_fact_not_confirmed);
    }

    /** "Canned beans in Earthquake kit: 5 cans before, 5 cans after." */
    @NonNull
    private String total(@NonNull BatchInfo batch, @NonNull String place,
                         @NonNull StockChange.Amounts amounts) {
        return getApplication().getString(R.string.stock_review_destination, batch.getName(), place,
                amount(amounts.getBefore(), batch), amount(amounts.getAfter(), batch));
    }

    // ----- Saving -----

    /** The request for the save, with the history of every batch it changes. */
    @NonNull
    private SwapStore.Request request(@NonNull RuleRunner.ShownSwap swap) {
        Context context = getApplication();
        BatchInfo kit = swap.getKitBatch();
        BatchInfo pantry = swap.getPantryBatch();
        int amount = swap.getQuantity();
        String moving = amount(amount, kit);

        String kitHistory;
        String kitPartHistory = null;
        if (amount == kit.getQuantity()) {
            kitHistory = context.getString(R.string.history_swap_out_all, moving,
                    kit.getLocationName(), pantry.getLocationName());
        } else {
            kitHistory = context.getString(R.string.history_swap_out_some, moving,
                    pantry.getLocationName(), amount(kit.getQuantity(), kit),
                    amount(kit.getQuantity() - amount, kit));
            kitPartHistory = context.getString(R.string.history_swap_out_new, moving,
                    kit.getLocationName());
        }
        String pantryHistory;
        String pantryPartHistory = null;
        if (amount == pantry.getQuantity()) {
            pantryHistory = context.getString(R.string.history_swap_in_all, moving,
                    pantry.getLocationName(), kit.getLocationName());
        } else {
            pantryHistory = context.getString(R.string.history_swap_in_some, moving,
                    kit.getLocationName(), amount(pantry.getQuantity(), pantry),
                    amount(pantry.getQuantity() - amount, pantry));
            pantryPartHistory = context.getString(R.string.history_swap_in_new, moving,
                    pantry.getLocationName());
        }
        return new SwapStore.Request(actionId, swap, kitHistory, kitPartHistory, pantryHistory,
                pantryPartHistory);
    }

    // ----- Small helpers -----

    @NonNull
    private String amount(int amount, @NonNull BatchInfo batch) {
        return BatchText.amount(getApplication(), amount, batch.getUnit());
    }

    /** "1 day" or "30 days", a number from Settings. */
    @NonNull
    private String dayCount(int days) {
        return getApplication().getResources().getQuantityString(R.plurals.day_count, days, days);
    }

    @NonNull
    private static String bullets(@NonNull List<String> lines) {
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append("• ").append(line);
        }
        return text.toString();
    }
}

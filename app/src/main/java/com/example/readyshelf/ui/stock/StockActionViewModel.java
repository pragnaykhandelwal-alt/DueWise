package com.example.readyshelf.ui.stock;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.readyshelf.AppContainer;
import com.example.readyshelf.R;
import com.example.readyshelf.ReadyShelfApp;
import com.example.readyshelf.data.BatchEventType;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.data.ReadyShelfRepository.SaveListener;
import com.example.readyshelf.data.ReadyShelfRepository.SaveResult;
import com.example.readyshelf.data.StorageLocation;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.StockChange;
import com.example.readyshelf.ui.common.BatchText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Prepares one stock change (move, used, discarded, or archive), its review, and saves it.
 *
 * <p>The choices and the review are kept in a SavedStateHandle, so they survive screen
 * rotation. The amount field keeps its own text.
 */
public class StockActionViewModel extends AndroidViewModel {

    /** The four stock changes. */
    public enum Action {
        MOVE,
        USED,
        DISCARDED,
        ARCHIVE
    }

    // Keys for the values kept in the SavedStateHandle.
    private static final String KEY_SOME = "some";
    private static final String KEY_DESTINATION_ID = "destinationId";
    private static final String KEY_REVIEW_AMOUNT = "reviewAmount";
    private static final String KEY_REVIEW_QUANTITY = "reviewQuantity";

    /** Everything the screen shows. */
    public static final class State {
        public boolean loaded;
        @Nullable public BatchInfo batch;        // null when the batch no longer exists
        public boolean reviewing;
        @NonNull public String reviewText = "";
        @Nullable public String kitWarning;      // kit amounts and "Replacement may be needed"
        public boolean saving;
        @StringRes public int error;             // 0 when there is no error
        public boolean done;                     // saved, so the screen closes
        @Nullable public String doneMessage;     // when done: what was recorded, for the next screen
    }

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final SavedStateHandle saved;
    private final LiveData<List<BatchInfo>> activeBatches;
    private final LiveData<List<StorageLocation>> locations;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final Executor mainThread;

    private boolean started;
    private Action action = Action.USED;
    @Nullable private LiveData<BatchInfo> batch;
    private boolean batchLoaded;
    private boolean saving;
    @StringRes private int error;
    private boolean done;
    // What the change records, written when it is confirmed and shown once it is saved.
    @Nullable private String doneMessage;

    public StockActionViewModel(@NonNull Application application, @NonNull SavedStateHandle savedState) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        saved = savedState;
        activeBatches = data.repository.observeBatches();
        locations = data.repository.observeLocations();
        mainThread = ContextCompat.getMainExecutor(application);
    }

    /** The Activity calls this in onCreate. Later calls do nothing. */
    public void start(long batchId, @NonNull Action actionToDo) {
        if (started) {
            return;
        }
        started = true;
        action = actionToDo;
        batch = data.repository.observeBatchInfo(batchId);
        state.addSource(batch, value -> {
            batchLoaded = true;
            publish();
        });
        state.addSource(activeBatches, value -> publish());
        state.addSource(locations, value -> publish());
    }

    @NonNull
    public Action getAction() {
        return action;
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    @NonNull
    public LiveData<List<StorageLocation>> getLocations() {
        return locations;
    }

    /** True when "Some" is chosen instead of "All". */
    public boolean isSome() {
        Boolean some = saved.get(KEY_SOME);
        return some != null && some;
    }

    public void setSome(boolean some) {
        if (some != isSome()) {
            saved.set(KEY_SOME, some);
            error = 0;
            publish();
        }
    }

    /** The chosen destination of a move, or 0 when none is chosen. */
    public long getDestinationId() {
        Long id = saved.get(KEY_DESTINATION_ID);
        return id != null ? id : 0;
    }

    public void setDestinationId(long destinationId) {
        saved.set(KEY_DESTINATION_ID, destinationId);
    }

    /**
     * Shows the review for {@code amount}. The Activity has already checked the amount and the
     * destination. The batch's quantity now is remembered, so nothing is saved if it changes.
     */
    public void startReview(int amount) {
        BatchInfo current = currentBatch();
        if (current == null) {
            return;
        }
        saved.set(KEY_REVIEW_AMOUNT, amount);
        saved.set(KEY_REVIEW_QUANTITY, current.getQuantity());
        error = 0;
        publish();
    }

    /** Goes back from the review to the amount. Nothing was saved. */
    public void cancelReview() {
        if (saving) {
            return;
        }
        saved.set(KEY_REVIEW_AMOUNT, null);
        saved.set(KEY_REVIEW_QUANTITY, null);
        error = 0;
        publish();
    }

    /** True when Archive is the action: it has no amount step, so it starts with the review. */
    public boolean isArchive() {
        return action == Action.ARCHIVE;
    }

    /** Saves the reviewed change. The screen closes when it is saved. */
    public void confirm() {
        BatchInfo current = currentBatch();
        if (current == null || saving || done) {
            return;
        }
        Context context = getApplication();
        SaveListener listener = result -> mainThread.execute(() -> saveFinished(result));
        if (isArchive()) {
            saving = true;
            doneMessage = context.getString(R.string.confirm_archived);
            data.repository.archiveBatch(current.getId(), current.getQuantity(),
                    context.getString(R.string.history_archived, amount(current, current.getQuantity()),
                            current.getLocationName()),
                    listener);
            publish();
            return;
        }
        Integer amount = saved.get(KEY_REVIEW_AMOUNT);
        Integer expected = saved.get(KEY_REVIEW_QUANTITY);
        if (amount == null || expected == null) {
            return;
        }
        int left = expected - amount;
        saving = true;
        if (action == Action.MOVE) {
            String destination = locationName(getDestinationId());
            doneMessage = context.getString(R.string.confirm_moved, amount(current, amount),
                    destination);
            String history;
            String newBatchHistory = null;
            if (left == 0) {
                history = context.getString(R.string.history_moved_all, amount(current, amount),
                        current.getLocationName(), destination);
            } else {
                history = context.getString(R.string.history_moved_some, amount(current, amount),
                        destination, amount(current, expected), amount(current, left));
                newBatchHistory = context.getString(R.string.history_moved_in,
                        amount(current, amount), current.getLocationName());
            }
            data.repository.moveBatch(current.getId(), expected, amount, getDestinationId(),
                    history, newBatchHistory, listener);
        } else {
            boolean used = action == Action.USED;
            doneMessage = context.getString(used ? R.string.confirm_used : R.string.confirm_discarded,
                    amount(current, amount));
            String history = context.getString(used ? R.string.history_used : R.string.history_discarded,
                    amount(current, amount), amount(current, expected), amount(current, left));
            data.repository.removeFromBatch(current.getId(), expected, amount,
                    used ? BatchEventType.USED : BatchEventType.DISCARDED, history, listener);
        }
        publish();
    }

    private void saveFinished(@NonNull SaveResult result) {
        saving = false;
        if (result == SaveResult.SAVED) {
            done = true;
        } else {
            error = result == SaveResult.BATCH_CHANGED ? R.string.error_batch_changed
                    : R.string.error_not_saved;
            if (result == SaveResult.BATCH_CHANGED && !isArchive()) {
                // Back to the amount step, so the household checks the new amounts.
                saved.set(KEY_REVIEW_AMOUNT, null);
                saved.set(KEY_REVIEW_QUANTITY, null);
            }
        }
        publish();
    }

    @Nullable
    private BatchInfo currentBatch() {
        return batch != null ? batch.getValue() : null;
    }

    private void publish() {
        State next = new State();
        next.loaded = batchLoaded;
        next.batch = currentBatch();
        next.saving = saving;
        next.error = error;
        next.done = done;
        next.doneMessage = done ? doneMessage : null;
        BatchInfo current = next.batch;
        if (current != null && !current.isArchived() && !done) {
            if (isArchive()) {
                next.reviewing = true;
                buildArchiveReview(current, next);
            } else {
                Integer amount = saved.get(KEY_REVIEW_AMOUNT);
                Integer expected = saved.get(KEY_REVIEW_QUANTITY);
                if (amount != null && expected != null && expected == current.getQuantity()) {
                    next.reviewing = true;
                    buildReview(current, amount, next);
                }
            }
        }
        state.setValue(next);
    }

    private void buildArchiveReview(@NonNull BatchInfo current, @NonNull State next) {
        Context context = getApplication();
        next.reviewText = "• " + context.getString(R.string.stock_review_archive, current.getName(),
                amount(current, current.getQuantity()), current.getLocationName());
        next.kitWarning = kitWarning(current, StockChange.archive(current, active()));
    }

    private void buildReview(@NonNull BatchInfo current, int amount, @NonNull State next) {
        Context context = getApplication();
        List<String> lines = new ArrayList<>();
        StockChange change;
        if (action == Action.MOVE) {
            long destinationId = getDestinationId();
            if (destinationId == 0 || destinationId == current.getLocationId()) {
                return;
            }
            change = StockChange.move(current, amount, destinationId, active());
            String destination = locationName(destinationId);
            lines.add(context.getString(R.string.stock_review_move, amount(current, amount),
                    current.getName(), current.getLocationName(), destination));
            int left = change.getBatch().getAfter();
            if (left == 0) {
                lines.add(context.getString(R.string.stock_review_move_all, current.getLocationName()));
            } else {
                lines.add(context.getString(R.string.stock_review_move_some, amount(current, left),
                        current.getLocationName(), amount(current, amount), destination));
            }
            StockChange.Amounts there = change.getDestination();
            if (there != null) {
                lines.add(context.getString(R.string.stock_review_destination, current.getName(),
                        destination, amount(current, there.getBefore()), amount(current, there.getAfter())));
            }
        } else {
            change = StockChange.removal(current, amount, active());
            lines.add(context.getString(action == Action.USED ? R.string.stock_review_used
                    : R.string.stock_review_discarded, amount(current, amount), current.getName()));
            lines.add(context.getString(R.string.stock_review_batch,
                    amount(current, change.getBatch().getBefore()),
                    amount(current, change.getBatch().getAfter())));
        }
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append("• ").append(line);
        }
        next.reviewText = text.toString();
        next.kitWarning = kitWarning(current, change);
    }

    /** "Canned beans in Earthquake kit: 6 cans before, 4 cans after. Replacement may be needed…" */
    @Nullable
    private String kitWarning(@NonNull BatchInfo current, @NonNull StockChange change) {
        StockChange.Amounts kit = change.getKit();
        if (kit == null) {
            return null;
        }
        Context context = getApplication();
        return context.getString(R.string.kit_amounts, current.getName(), current.getLocationName(),
                amount(current, kit.getBefore()), amount(current, kit.getAfter()))
                + " " + context.getString(R.string.kit_warning);
    }

    @NonNull
    private List<BatchInfo> active() {
        List<BatchInfo> list = activeBatches.getValue();
        return list != null ? list : Collections.emptyList();
    }

    @NonNull
    private String amount(@NonNull BatchInfo batchInfo, int quantity) {
        return BatchText.amount(getApplication(), quantity, batchInfo.getUnit());
    }

    @NonNull
    private String locationName(long id) {
        List<StorageLocation> current = locations.getValue();
        if (current != null) {
            for (StorageLocation location : current) {
                if (location.id == id) {
                    return location.name;
                }
            }
        }
        return "#" + id;
    }
}

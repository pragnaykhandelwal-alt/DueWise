package com.example.readyshelf.ui.batch;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.color.MaterialColors;

import com.example.readyshelf.R;
import com.example.readyshelf.data.BatchEvent;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.CategoryLook;
import com.example.readyshelf.ui.common.DisplayNames;
import com.example.readyshelf.ui.common.LargeText;
import com.example.readyshelf.ui.common.PlaceLook;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.common.StatusLook;
import com.example.readyshelf.ui.stock.StockActionActivity;
import com.example.readyshelf.ui.stock.StockActionViewModel.Action;

import java.util.List;

/**
 * One batch: product, quantity, location, category, date, status, and history. A header card
 * shows the batch at a glance, and a card lists the facts with icons. "Record used" is the main
 * button; Edit, Move, Record discarded, and Archive are smaller. The history is a timeline.
 * Since 2026-10-22 there is no food card (the student's choice): older batches keep their food
 * values, but no screen shows them.
 */
public class BatchDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_BATCH_ID = "com.example.readyshelf.BATCH_ID";

    /** Opens the details of one batch. */
    @NonNull
    public static Intent newIntent(@NonNull Context context, long batchId) {
        return new Intent(context, BatchDetailsActivity.class).putExtra(EXTRA_BATCH_ID, batchId);
    }

    private BatchDetailsViewModel viewModel;
    private View content;
    private TextView missingText;
    private ImageView categoryIcon;
    private TextView nameText;
    private TextView productText;
    private TextView sampleTag;
    private TextView archivedTag;
    private TextView quantityText;
    private TextView locationText;
    private TextView categoryText;
    private TextView dateText;
    private TextView dateReviewText;
    private TextView statusLabel;
    private TextView statusText;
    private TextView actionNote;
    private Button usedButton;
    private Button discardedButton;
    private Button moveButton;
    private Button archiveButton;
    private View actionsRow2;
    private TextView historyEmpty;
    private LinearLayout historyList;
    // The icons before the fact rows, in a quiet color; the words say everything.
    private int rowIconColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_batch_details);
        viewModel = new ViewModelProvider(this).get(BatchDetailsViewModel.class);
        viewModel.start(getIntent().getLongExtra(EXTRA_BATCH_ID, 0));

        content = findViewById(R.id.details_content);
        missingText = findViewById(R.id.details_missing);
        categoryIcon = findViewById(R.id.details_category_icon);
        nameText = findViewById(R.id.details_name);
        productText = findViewById(R.id.details_product);
        sampleTag = findViewById(R.id.details_sample);
        archivedTag = findViewById(R.id.details_archived);
        quantityText = findViewById(R.id.details_quantity);
        locationText = findViewById(R.id.details_location);
        categoryText = findViewById(R.id.details_category);
        dateText = findViewById(R.id.details_date);
        dateReviewText = findViewById(R.id.details_date_review);
        statusLabel = findViewById(R.id.details_status_label);
        statusText = findViewById(R.id.details_status);
        StatusLook.setStartIcon(archivedTag, R.drawable.ic_archive, MaterialColors.getColor(
                archivedTag, com.google.android.material.R.attr.colorOnSurfaceVariant));
        actionNote = findViewById(R.id.details_action_note);
        usedButton = findViewById(R.id.details_used);
        discardedButton = findViewById(R.id.details_discarded);
        moveButton = findViewById(R.id.details_move);
        archiveButton = findViewById(R.id.details_archive);
        actionsRow2 = findViewById(R.id.details_actions_row2);
        historyEmpty = findViewById(R.id.details_history_empty);
        historyList = findViewById(R.id.details_history);
        rowIconColor = MaterialColors.getColor(nameText,
                com.google.android.material.R.attr.colorOnSurfaceVariant);

        // With large text, the two buttons of each row go under each other.
        int gap = getResources().getDimensionPixelSize(R.dimen.space_small);
        LargeText.stackIfLarge(findViewById(R.id.details_actions_row1), gap);
        LargeText.stackIfLarge(findViewById(R.id.details_actions_row2), gap);

        findViewById(R.id.details_edit).setOnClickListener(view -> startActivity(
                BatchEditActivity.newEditIntent(this, viewModel.getBatchId())));
        usedButton.setOnClickListener(view -> openStockAction(Action.USED));
        discardedButton.setOnClickListener(view -> openStockAction(Action.DISCARDED));
        moveButton.setOnClickListener(view -> openStockAction(Action.MOVE));
        archiveButton.setOnClickListener(view -> openStockAction(Action.ARCHIVE));

        viewModel.getState().observe(this, this::show);
        viewModel.getHistory().observe(this, this::showHistory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshToday();
    }

    private void openStockAction(@NonNull Action action) {
        startActivity(StockActionActivity.newIntent(this, viewModel.getBatchId(), action));
    }

    private void show(BatchDetailsViewModel.State state) {
        BatchInfo batch = state.batch;
        if (batch == null) {
            missingText.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            return;
        }
        missingText.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        categoryIcon.setImageResource(CategoryLook.icon(batch.getCategory()));
        nameText.setText(batch.getName());
        productText.setText(BatchText.productFacts(this, batch.getVariant(), batch.getPackageSize(),
                batch.getUnit()));
        sampleTag.setVisibility(batch.isSample() ? View.VISIBLE : View.GONE);
        archivedTag.setVisibility(batch.isArchived() ? View.VISIBLE : View.GONE);
        quantityText.setText(getString(R.string.details_quantity,
                BatchText.amount(this, batch.getQuantity(), batch.getUnit())));
        StatusLook.setStartIcon(quantityText, R.drawable.ic_inventory, rowIconColor);
        locationText.setText(getString(R.string.details_location, batch.getLocationName(),
                getString(DisplayNames.locationType(batch.getLocationType()))));
        StatusLook.setStartIcon(locationText, PlaceLook.icon(batch.getLocationType()), rowIconColor);
        categoryText.setText(getString(R.string.details_category,
                getString(DisplayNames.category(batch.getCategory()))));
        StatusLook.setStartIcon(categoryText, CategoryLook.icon(batch.getCategory()), rowIconColor);
        dateText.setText(BatchText.dateEntry(this, batch));
        StatusLook.setStartIcon(dateText, R.drawable.ic_calendar, rowIconColor);
        String dateReview = BatchText.dateReview(this, batch);
        dateReviewText.setText(dateReview);
        dateReviewText.setVisibility(dateReview == null ? View.GONE : View.VISIBLE);

        // A status shows as a label; otherwise a sentence says why there is none.
        boolean hasStatus = !batch.isArchived() && state.rulesError == null && state.status != null;
        statusLabel.setVisibility(hasStatus ? View.VISIBLE : View.GONE);
        statusText.setVisibility(hasStatus ? View.GONE : View.VISIBLE);
        if (hasStatus) {
            StatusLook.showLabel(statusLabel, state.status);
        } else if (batch.isArchived()) {
            statusText.setText(R.string.details_status_archived);
        } else if (state.rulesError != null) {
            statusText.setText(R.string.rules_error); // the details are in the phone's log
        } else {
            statusText.setText(R.string.details_status_off);
        }

        showActions(batch);
    }

    /**
     * An archived batch can only be edited. A batch with 0 left can be edited or archived:
     * there is nothing to use, discard, or move.
     */
    private void showActions(@NonNull BatchInfo batch) {
        boolean archived = batch.isArchived();
        boolean empty = batch.getQuantity() == 0;
        int stockVisibility = archived ? View.GONE : View.VISIBLE;
        usedButton.setVisibility(stockVisibility);
        discardedButton.setVisibility(stockVisibility);
        moveButton.setVisibility(stockVisibility);
        archiveButton.setVisibility(stockVisibility);
        // Its two buttons are hidden then, so the row takes no space either.
        actionsRow2.setVisibility(stockVisibility);
        usedButton.setEnabled(!empty);
        discardedButton.setEnabled(!empty);
        moveButton.setEnabled(!empty);
        if (archived) {
            actionNote.setText(R.string.details_archived_note);
            actionNote.setVisibility(View.VISIBLE);
        } else if (empty) {
            actionNote.setText(R.string.details_zero_left);
            actionNote.setVisibility(View.VISIBLE);
        } else {
            actionNote.setVisibility(View.GONE);
        }
    }

    /**
     * Adds one timeline step per history entry. A batch has few entries, so a LinearLayout is
     * enough. The line stops at the first and the last entry.
     */
    private void showHistory(List<BatchEvent> events) {
        historyList.removeAllViews();
        historyEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < events.size(); i++) {
            BatchEvent event = events.get(i);
            View row = inflater.inflate(R.layout.item_history, historyList, false);
            TextView heading = row.findViewById(R.id.history_heading);
            TextView details = row.findViewById(R.id.history_details);
            heading.setText(getString(R.string.history_heading,
                    getString(DisplayNames.eventType(event.type)), BatchText.dateTime(event.occurredAtMillis)));
            details.setText(event.details);
            row.findViewById(R.id.history_line_top)
                    .setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
            row.findViewById(R.id.history_line_bottom)
                    .setVisibility(i == events.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            historyList.addView(row);
        }
    }
}

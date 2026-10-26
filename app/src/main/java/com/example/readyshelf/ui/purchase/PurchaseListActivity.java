package com.example.readyshelf.ui.purchase;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readyshelf.R;
import com.example.readyshelf.data.PurchaseItemInfo;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.PurchaseSuggestions;
import com.example.readyshelf.ui.batch.BatchDetailsActivity;
import com.example.readyshelf.ui.batch.BatchEditActivity;
import com.example.readyshelf.ui.common.BatchAdapter;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.Confirmations;
import com.example.readyshelf.ui.common.MessageText;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The Purchase/Renewal list ("Purchase list" until 2026-10-24): the purchase list, the replacement
 * needs (kit items the household buys again that the swaps don't cover, and expired ones in any
 * place), and Renewal (documents and subscriptions dated today or earlier). Each need's card can
 * add its product and place to the list. Every change to the list is confirmed in a dialog first,
 * and none of them changes stock. A bought item enters the stock only through "Record as bought"
 * (a new batch with its date). Renewal's "Renewed" and "Discard" do change the batch, after a
 * dialog. Until 2026-10-22 a "Suggestions" section added items to the list; the student removed it.
 */
public class PurchaseListActivity extends AppCompatActivity {

    private PurchaseListViewModel viewModel;
    private BatchAdapter needsAdapter;
    private BatchAdapter renewalAdapter;
    private TextView resultText;
    private TextView entriesMessage;
    private LinearLayout entriesList;
    private TextView needsMessage;
    private TextView renewalMessage;
    // The cards on screen. They are rebuilt only when their content changes, so TalkBack
    // doesn't lose its place each time the database sends new values.
    @Nullable private List<PurchaseListViewModel.EntryRow> shownEntries;
    private boolean shownBusy;
    // A dialog that is open now. It is closed in onDestroy, so it can't leak when the screen rotates.
    @Nullable private Dialog openDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_purchase_list);
        viewModel = new ViewModelProvider(this).get(PurchaseListViewModel.class);

        resultText = findViewById(R.id.purchase_result);
        entriesMessage = findViewById(R.id.purchase_list_message);
        entriesList = findViewById(R.id.purchase_entries);
        needsMessage = findViewById(R.id.purchase_message);
        // A tap on a need's card opens Batch details; its "Add to purchase list" asks first.
        needsAdapter = new BatchAdapter(
                batch -> startActivity(BatchDetailsActivity.newIntent(this, batch.getId())),
                row -> {
                    PurchaseSuggestions.Suggestion total = viewModel.totalFor(row.getBatch());
                    if (total != null) {
                        confirmAdd(total);
                    }
                });
        RecyclerView needsList = findViewById(R.id.purchase_list);
        ScreenSetup.setUpList(needsList, needsAdapter);

        renewalMessage = findViewById(R.id.purchase_renewal_message);
        // A tap on a Renewal card opens Batch details. "Renewed" asks for the new date, then
        // asks to save it; "Discard" asks first too.
        renewalAdapter = new BatchAdapter(
                batch -> startActivity(BatchDetailsActivity.newIntent(this, batch.getId())),
                row -> pickRenewalDate(row.getBatch()),
                row -> confirmDiscard(row.getBatch()));
        RecyclerView renewalList = findViewById(R.id.purchase_renewals);
        ScreenSetup.setUpList(renewalList, renewalAdapter);

        viewModel.getState().observe(this, this::show);
        viewModel.getSavedMessage().observe(this, message -> {
            if (message != null) {
                Confirmations.show(this, message);
                viewModel.savedMessageShown();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshToday();
    }

    @Override
    protected void onDestroy() {
        if (openDialog != null) {
            openDialog.dismiss();
        }
        super.onDestroy();
    }

    private void show(PurchaseListViewModel.State state) {
        if (state.result != null) {
            resultText.setText(state.result);
            resultText.setVisibility(View.VISIBLE);
        } else {
            resultText.setVisibility(View.GONE);
        }
        boolean busyChanged = state.busy != shownBusy;
        shownBusy = state.busy;
        if (busyChanged || !state.entries.equals(shownEntries)) {
            showEntries(state.entries, state.busy);
        }
        MessageText.show(entriesMessage, state.entriesMessage, null);
        needsAdapter.submitList(state.needRows);
        MessageText.show(needsMessage, state.needsMessage, state.rulesError);
        renewalAdapter.submitList(state.renewalRows);
        MessageText.show(renewalMessage, state.renewalMessage, state.renewalError);
    }

    // ----- Items on the list -----

    private void showEntries(@NonNull List<PurchaseListViewModel.EntryRow> entries, boolean busy) {
        shownEntries = new ArrayList<>(entries);
        entriesList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (PurchaseListViewModel.EntryRow row : entries) {
            PurchaseItemInfo item = row.item;
            View card = inflater.inflate(R.layout.item_purchase_entry, entriesList, false);
            TextView name = card.findViewById(R.id.purchase_entry_name);
            TextView details = card.findViewById(R.id.purchase_entry_details);
            TextView amountLine = card.findViewById(R.id.purchase_entry_amount);
            TextView suggested = card.findViewById(R.id.purchase_entry_suggested);
            Button use = card.findViewById(R.id.purchase_entry_use);
            Button bought = card.findViewById(R.id.purchase_entry_bought);
            Button remove = card.findViewById(R.id.purchase_entry_remove);

            String amount = BatchText.amount(this, item.getQuantity(), item.getUnit());
            name.setText(item.getName());
            details.setText(BatchText.productChoiceDetails(this, item.getVariant(),
                    item.getPackageSize(), item.getUnit()));
            amountLine.setText(getString(R.string.purchase_entry_amount, amount, item.getLocationName()));

            // "Needed now": what the replacement needs add up to, from the current stock.
            if (row.needsKnown) {
                suggested.setText(row.neededNow != null
                        ? getString(R.string.purchase_entry_needed_now,
                        BatchText.amount(this, row.neededNow, item.getUnit()))
                        : getString(R.string.purchase_entry_not_needed));
                suggested.setVisibility(View.VISIBLE);
            } else {
                suggested.setVisibility(View.GONE);
            }
            Integer newQuantity = row.neededNow;
            if (row.needsKnown && newQuantity != null && newQuantity != item.getQuantity()) {
                String newAmount = BatchText.amount(this, newQuantity, item.getUnit());
                use.setText(getString(R.string.purchase_entry_use, newAmount));
                use.setContentDescription(getString(R.string.purchase_entry_use_description,
                        item.getName(), item.getLocationName(), newAmount));
                use.setOnClickListener(view -> confirmChange(row, newQuantity));
                use.setVisibility(View.VISIBLE);
            } else {
                use.setVisibility(View.GONE);
            }
            bought.setContentDescription(getString(R.string.purchase_entry_bought_description,
                    item.getName(), item.getLocationName()));
            bought.setOnClickListener(view -> startActivity(BatchEditActivity.newPurchaseIntent(
                    this, item.getId(), item.getProductId(), item.getKitLocationId(),
                    item.getQuantity())));
            remove.setContentDescription(getString(R.string.purchase_entry_remove_description,
                    item.getName(), item.getLocationName()));
            remove.setOnClickListener(view -> confirmRemove(row));
            use.setEnabled(!busy);
            bought.setEnabled(!busy);
            remove.setEnabled(!busy);
            entriesList.addView(card);
        }
    }

    // ----- Confirmation dialogs. The amounts are taken when the dialog opens. -----

    private void confirmAdd(@NonNull PurchaseSuggestions.Suggestion suggestion) {
        BatchInfo batch = suggestion.getFirstBatch();
        String message = getString(R.string.purchase_add_message,
                BatchText.amount(this, suggestion.getQuantity(), batch.getUnit()),
                BatchText.productChoice(this, batch), batch.getLocationName());
        showDialog(R.string.purchase_add_title, message, R.string.purchase_add_confirm,
                () -> viewModel.add(suggestion));
    }

    private void confirmChange(@NonNull PurchaseListViewModel.EntryRow row, int newQuantity) {
        PurchaseItemInfo item = row.item;
        String message = getString(R.string.purchase_use_message, viewModel.productText(item),
                item.getLocationName(), BatchText.amount(this, item.getQuantity(), item.getUnit()),
                BatchText.amount(this, newQuantity, item.getUnit()));
        showDialog(R.string.purchase_use_title, message, R.string.purchase_use_confirm,
                () -> viewModel.changeQuantity(row, newQuantity));
    }

    private void confirmRemove(@NonNull PurchaseListViewModel.EntryRow row) {
        PurchaseItemInfo item = row.item;
        String message = getString(R.string.purchase_remove_message,
                BatchText.amount(this, item.getQuantity(), item.getUnit()),
                viewModel.productText(item), item.getLocationName());
        showDialog(R.string.purchase_remove_title, message, R.string.purchase_remove_confirm,
                () -> viewModel.remove(row));
    }

    // ----- Renewal -----

    /**
     * "Renewed": Android's date picker, from tomorrow on, because a renewed date is after today.
     * The picked date is saved only after the household confirms it.
     */
    private void pickRenewalDate(@NonNull BatchInfo batch) {
        if (openDialog != null && openDialog.isShowing()) {
            return; // one dialog at a time
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        // DatePickerDialog counts months from 0 (January is 0). LocalDate counts from 1.
        DatePickerDialog dialog = new DatePickerDialog(this,
                (picker, year, month, dayOfMonth) -> {
                    // The picker is still showing while it calls this; it closes right after.
                    openDialog = null;
                    confirmRenewal(batch, LocalDate.of(year, month + 1, dayOfMonth));
                },
                tomorrow.getYear(), tomorrow.getMonthValue() - 1, tomorrow.getDayOfMonth());
        dialog.getDatePicker().setMinDate(
                tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        openDialog = dialog;
        dialog.show();
    }

    private void confirmRenewal(@NonNull BatchInfo batch, @NonNull LocalDate newDate) {
        String message = getString(R.string.renewal_renewed_message, batch.getName(),
                batch.getLocationName(), BatchText.dateEntry(this, batch),
                BatchText.labeledDate(this, newDate, batch.getDateLabel()));
        showDialog(R.string.renewal_renewed_title, message, R.string.renewal_renewed_confirm,
                () -> viewModel.renew(batch, newDate));
    }

    private void confirmDiscard(@NonNull BatchInfo batch) {
        String message = getString(R.string.renewal_discard_message, batch.getName(),
                batch.getLocationName());
        showDialog(R.string.renewal_discard_title, message, R.string.renewal_discard_confirm,
                () -> viewModel.discard(batch));
    }

    /** Shows one confirmation dialog. Nothing happens unless the household taps the button. */
    private void showDialog(int title, @NonNull String message, int confirmText,
                            @NonNull Runnable onConfirm) {
        if (openDialog != null && openDialog.isShowing()) {
            return; // one dialog at a time
        }
        openDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(confirmText, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

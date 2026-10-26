package com.example.readyshelf.ui.stock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.example.readyshelf.R;
import com.example.readyshelf.data.StorageLocation;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.StockChange;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.Confirmations;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.common.Spinners;
import com.example.readyshelf.ui.stock.StockActionViewModel.Action;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Changes the stock of one batch: Move, Record used, Record discarded, or Archive.
 * First the household says how much (and where to), then checks a review, then confirms.
 * Nothing changes before "Confirm".
 */
public class StockActionActivity extends AppCompatActivity {

    private static final String EXTRA_BATCH_ID = "com.example.readyshelf.BATCH_ID";
    private static final String EXTRA_ACTION = "com.example.readyshelf.STOCK_ACTION";

    /** Opens the screen for one stock change of one batch. */
    @NonNull
    public static Intent newIntent(@NonNull Context context, long batchId, @NonNull Action action) {
        return new Intent(context, StockActionActivity.class)
                .putExtra(EXTRA_BATCH_ID, batchId)
                .putExtra(EXTRA_ACTION, action.name());
    }

    private StockActionViewModel viewModel;
    private NestedScrollView scrollView;
    private TextView messageText;
    private View content;
    private TextView nameText;
    private TextView productDetailsText;
    private TextView sampleTag;
    private TextView amountLine;
    private TextView dateLine;
    private TextView medicineNote;
    private View amountStep;
    private RadioGroup allOrSomeGroup;
    private RadioButton allChoice;
    private TextInputLayout amountLayout;
    private EditText amountField;
    private View moveGroup;
    private Spinner destinationSpinner;
    private TextView destinationError;
    private View reviewStep;
    private TextView reviewText;
    private TextView kitWarning;
    private TextView errorText;
    // The bar at the bottom with the step's main button: "Review", then "Confirm".
    private View bottomBar;
    private Button reviewButton;
    private Button confirmButton;
    private Button backButton;
    // The error on screen (0 for none), so a new one scrolls into view only once.
    @StringRes private int shownError;
    // The locations in the same order as the destination list.
    private final List<StorageLocation> shownLocations = new ArrayList<>();
    // While the review is shown, Back returns to the amount step instead of closing the screen.
    private OnBackPressedCallback backToAmount;
    private boolean reviewWasShown;
    // The change is saved and the screen is closing, so its save message is left only once.
    private boolean closing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_stock_action);
        viewModel = new ViewModelProvider(this).get(StockActionViewModel.class);
        Action action = Action.valueOf(getIntent().getStringExtra(EXTRA_ACTION));
        viewModel.start(getIntent().getLongExtra(EXTRA_BATCH_ID, 0), action);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(titleFor(action));
        setTitle(titleFor(action));

        scrollView = findViewById(R.id.stock_scroll);
        messageText = findViewById(R.id.stock_message);
        content = findViewById(R.id.stock_content);
        nameText = findViewById(R.id.stock_name);
        productDetailsText = findViewById(R.id.stock_product_details);
        sampleTag = findViewById(R.id.stock_sample);
        amountLine = findViewById(R.id.stock_amount_line);
        dateLine = findViewById(R.id.stock_date_line);
        medicineNote = findViewById(R.id.stock_medicine_note);
        amountStep = findViewById(R.id.stock_amount_step);
        allOrSomeGroup = findViewById(R.id.stock_all_or_some);
        allChoice = findViewById(R.id.stock_all);
        RadioButton someChoice = findViewById(R.id.stock_some);
        someChoice.setContentDescription(getString(R.string.stock_how_much_answer, someChoice.getText()));
        amountLayout = findViewById(R.id.stock_amount_layout);
        amountField = findViewById(R.id.stock_amount);
        moveGroup = findViewById(R.id.stock_move_group);
        destinationSpinner = findViewById(R.id.stock_destination);
        destinationError = findViewById(R.id.stock_destination_error);
        reviewStep = findViewById(R.id.stock_review_step);
        reviewText = findViewById(R.id.stock_review_text);
        kitWarning = findViewById(R.id.stock_kit_warning);
        errorText = findViewById(R.id.stock_error);
        bottomBar = findViewById(R.id.bottom_actions);
        reviewButton = findViewById(R.id.stock_review_button);
        confirmButton = findViewById(R.id.stock_confirm);
        backButton = findViewById(R.id.stock_back);

        backToAmount = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.cancelReview();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToAmount);
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        allOrSomeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            viewModel.setSome(checkedId == R.id.stock_some);
            amountLayout.setError(null);
        });
        Spinners.setOnChoice(destinationSpinner, position -> {
            viewModel.setDestinationId(shownLocations.get(position).id);
            destinationError.setVisibility(View.GONE);
        });
        viewModel.getLocations().observe(this, this::showLocations);
        reviewButton.setOnClickListener(view -> review());
        confirmButton.setOnClickListener(view -> viewModel.confirm());
        // For Archive, Back leaves the screen: there is no amount step to go back to.
        backButton.setOnClickListener(view -> {
            if (viewModel.isArchive()) {
                finish();
            } else {
                viewModel.cancelReview();
            }
        });

        viewModel.getState().observe(this, this::show);
    }

    @StringRes
    private static int titleFor(@NonNull Action action) {
        switch (action) {
            case MOVE:
                return R.string.title_move;
            case USED:
                return R.string.title_record_used;
            case DISCARDED:
                return R.string.title_record_discarded;
            default:
                return R.string.title_archive;
        }
    }

    /** Fills the destination list with every location except the batch's own. */
    private void showLocations(List<StorageLocation> locations) {
        BatchInfo batch = currentBatch();
        if (batch == null) {
            return; // shown again when the batch has loaded
        }
        shownLocations.clear();
        List<String> labels = new ArrayList<>();
        int selected = 0;
        for (StorageLocation location : locations) {
            if (location.id != batch.getLocationId()) {
                if (location.id == viewModel.getDestinationId()) {
                    selected = shownLocations.size();
                }
                shownLocations.add(location);
                labels.add(location.name);
            }
        }
        Spinners.setChoices(destinationSpinner, labels, selected);
        if (!shownLocations.isEmpty()) {
            viewModel.setDestinationId(shownLocations.get(selected).id);
        }
    }

    private BatchInfo currentBatch() {
        StockActionViewModel.State state = viewModel.getState().getValue();
        return state != null ? state.batch : null;
    }

    private void show(StockActionViewModel.State state) {
        if (state.done) {
            if (!closing) {
                // Saved: the screen closes, and the screen underneath says what was recorded.
                closing = true;
                if (state.doneMessage != null) {
                    Confirmations.showOnNextScreen(this, state.doneMessage);
                }
                finish();
            }
            return;
        }
        if (!state.loaded) {
            return;
        }
        BatchInfo batch = state.batch;
        if (batch == null) {
            showOnlyMessage(R.string.details_missing);
            return;
        }
        if (batch.isArchived()) {
            showOnlyMessage(R.string.stock_archived);
            return;
        }
        boolean archive = viewModel.isArchive();
        if (!archive && batch.getQuantity() == 0) {
            showOnlyMessage(R.string.stock_nothing_left);
            return;
        }
        messageText.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);

        nameText.setText(batch.getName());
        String details = BatchText.productDetails(batch.getVariant(), batch.getPackageSize());
        productDetailsText.setText(details);
        productDetailsText.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);
        sampleTag.setVisibility(batch.isSample() ? View.VISIBLE : View.GONE);
        amountLine.setText(BatchText.amountInPlace(this, batch));
        dateLine.setText(BatchText.dateEntry(this, batch));
        boolean removal = viewModel.getAction() == Action.USED || viewModel.getAction() == Action.DISCARDED;
        medicineNote.setVisibility(removal && batch.getCategory() == ItemCategory.MEDICINE
                ? View.VISIBLE : View.GONE);

        // Step 1: how much, and where to.
        allChoice.setText(getString(R.string.stock_all,
                BatchText.amount(this, batch.getQuantity(), batch.getUnit())));
        // TalkBack reads the question with the answer: "How much? All (4 cans)".
        allChoice.setContentDescription(getString(R.string.stock_how_much_answer, allChoice.getText()));
        allOrSomeGroup.check(viewModel.isSome() ? R.id.stock_some : R.id.stock_all);
        amountLayout.setVisibility(viewModel.isSome() ? View.VISIBLE : View.GONE);
        amountLayout.setHint(batch.getUnit().isEmpty() ? getString(R.string.stock_amount)
                : getString(R.string.stock_amount_with_unit, batch.getUnit()));
        amountLayout.setHelperText(getString(R.string.stock_amount_helper, batch.getQuantity()));
        moveGroup.setVisibility(viewModel.getAction() == Action.MOVE ? View.VISIBLE : View.GONE);
        if (viewModel.getAction() == Action.MOVE && shownLocations.isEmpty()) {
            List<StorageLocation> locations = viewModel.getLocations().getValue();
            if (locations != null) {
                showLocations(locations);
            }
        }

        // Step 2: the review.
        boolean amountShown = !state.reviewing && !archive;
        amountStep.setVisibility(amountShown ? View.VISIBLE : View.GONE);
        reviewStep.setVisibility(state.reviewing ? View.VISIBLE : View.GONE);
        // The bar at the bottom shows the step's main button.
        reviewButton.setVisibility(amountShown ? View.VISIBLE : View.GONE);
        confirmButton.setVisibility(state.reviewing ? View.VISIBLE : View.GONE);
        backToAmount.setEnabled(state.reviewing && !archive);
        reviewText.setText(state.reviewText);
        kitWarning.setText(state.kitWarning);
        kitWarning.setVisibility(state.kitWarning == null ? View.GONE : View.VISIBLE);
        if (state.error != 0) {
            errorText.setText(state.error);
            errorText.setVisibility(View.VISIBLE);
            // The buttons are in the bar at the bottom, so the household may have scrolled away
            // from the error near the top: it scrolls into view once.
            if (state.error != shownError) {
                scrollView.smoothScrollTo(0, 0);
            }
        } else {
            errorText.setVisibility(View.GONE);
        }
        shownError = state.error;
        confirmButton.setEnabled(!state.saving);
        backButton.setEnabled(!state.saving);
        if (state.reviewing != reviewWasShown) {
            reviewWasShown = state.reviewing;
            scrollView.scrollTo(0, 0);
        }
    }

    private void showOnlyMessage(@StringRes int message) {
        messageText.setText(message);
        messageText.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
        backToAmount.setEnabled(false);
    }

    /** Checks the amount and the destination. If they are fine, shows the review. */
    private void review() {
        BatchInfo batch = currentBatch();
        if (batch == null) {
            return;
        }
        amountLayout.setError(null);
        destinationError.setVisibility(View.GONE);
        int amount = batch.getQuantity();
        boolean ok = true;
        if (viewModel.isSome()) {
            String text = amountField.getText() == null ? "" : amountField.getText().toString();
            Integer some = StockChange.parsePartAmount(text, batch.getQuantity());
            if (some == null) {
                amountLayout.setError(getString(R.string.error_stock_amount, batch.getQuantity()));
                amountField.requestFocus();
                ok = false;
            } else {
                amount = some;
            }
        }
        if (viewModel.getAction() == Action.MOVE) {
            long destinationId = viewModel.getDestinationId();
            if (shownLocations.isEmpty()) {
                destinationError.setText(R.string.stock_no_other_location);
                destinationError.setVisibility(View.VISIBLE);
                ok = false;
            } else if (destinationId == 0 || destinationId == batch.getLocationId()) {
                destinationError.setText(getString(R.string.error_stock_destination, batch.getLocationName()));
                destinationError.setVisibility(View.VISIBLE);
                ok = false;
            }
        }
        if (ok) {
            // "Review" is in the bar at the bottom, so the amount field can still have the
            // keyboard open. It closes, so it doesn't cover the review.
            InputMethodManager keyboard = getSystemService(InputMethodManager.class);
            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(scrollView.getWindowToken(), 0);
            }
            viewModel.startReview(amount);
        }
    }
}

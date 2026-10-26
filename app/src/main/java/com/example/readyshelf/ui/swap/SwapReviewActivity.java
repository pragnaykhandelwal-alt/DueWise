package com.example.readyshelf.ui.swap;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.example.readyshelf.R;
import com.example.readyshelf.ui.common.Confirmations;
import com.example.readyshelf.ui.common.MessageText;
import com.example.readyshelf.ui.common.MoreText;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.common.SwapDiagram;

/**
 * Review swap: shows one proposed swap in full, and saves it only when the household taps
 * "Confirm swap". The ViewModel does the work; this screen only shows its State.
 */
public class SwapReviewActivity extends AppCompatActivity {

    private static final String EXTRA_KIT_BATCH_ID = "com.example.readyshelf.KIT_BATCH_ID";
    private static final String EXTRA_PANTRY_BATCH_ID = "com.example.readyshelf.PANTRY_BATCH_ID";
    private static final String EXTRA_QUANTITY = "com.example.readyshelf.SWAP_QUANTITY";

    /** Opens the review of one swap that Home shows. */
    @NonNull
    public static Intent newIntent(@NonNull Context context, long kitBatchId, long pantryBatchId,
                                   int quantity) {
        return new Intent(context, SwapReviewActivity.class)
                .putExtra(EXTRA_KIT_BATCH_ID, kitBatchId)
                .putExtra(EXTRA_PANTRY_BATCH_ID, pantryBatchId)
                .putExtra(EXTRA_QUANTITY, quantity);
    }

    private SwapReviewViewModel viewModel;
    private TextView message;
    private View content;
    private TextView name;
    private TextView productDetails;
    private TextView sample;
    private TextView eachWay;
    private View diagram;
    // The two folded parts: every rule with what the household recorded, and what stays,
    // leaves, and arrives in each place.
    private View whyDetails;
    private View afterDetails;
    private TextView whyRules;
    private TextView facts;
    private View totals;
    private TextView kitPlace;
    private TextView kitBefore;
    private TextView kitAfter;
    private TextView pantryPlace;
    private TextView pantryBefore;
    private TextView pantryAfter;
    private TextView afterKit;
    private TextView afterPantry;
    private TextView afterNewBatch;
    private TextView resultText;
    private NestedScrollView scrollView;
    // The bar at the bottom with "Confirm swap".
    private View bottomBar;
    private Button confirmButton;
    private Button backButton;
    private boolean closing;
    // The message on screen after a tap on "Confirm swap" (0 for none), so a new one scrolls
    // into view only once.
    @StringRes private int shownResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_swap_review);
        viewModel = new ViewModelProvider(this).get(SwapReviewViewModel.class);
        Intent intent = getIntent();
        viewModel.start(intent.getLongExtra(EXTRA_KIT_BATCH_ID, 0),
                intent.getLongExtra(EXTRA_PANTRY_BATCH_ID, 0),
                intent.getIntExtra(EXTRA_QUANTITY, 0));

        message = findViewById(R.id.swap_message);
        content = findViewById(R.id.swap_content);
        name = findViewById(R.id.swap_name);
        productDetails = findViewById(R.id.swap_product_details);
        sample = findViewById(R.id.swap_sample);
        eachWay = findViewById(R.id.swap_each_way);
        diagram = findViewById(R.id.swap_diagram);
        whyDetails = findViewById(R.id.swap_why_details);
        afterDetails = findViewById(R.id.swap_after_details);
        whyRules = findViewById(R.id.swap_why_rules);
        facts = findViewById(R.id.swap_facts);
        totals = findViewById(R.id.swap_totals);
        kitPlace = findViewById(R.id.swap_totals_kit_place);
        kitBefore = findViewById(R.id.swap_totals_kit_before);
        kitAfter = findViewById(R.id.swap_totals_kit_after);
        pantryPlace = findViewById(R.id.swap_totals_pantry_place);
        pantryBefore = findViewById(R.id.swap_totals_pantry_before);
        pantryAfter = findViewById(R.id.swap_totals_pantry_after);
        afterKit = findViewById(R.id.swap_after_kit);
        afterPantry = findViewById(R.id.swap_after_pantry);
        afterNewBatch = findViewById(R.id.swap_after_new_batch);
        resultText = findViewById(R.id.swap_result);
        scrollView = findViewById(R.id.swap_scroll);
        bottomBar = findViewById(R.id.bottom_actions);
        confirmButton = findViewById(R.id.swap_confirm);
        backButton = findViewById(R.id.swap_back);

        MoreText.setUp(findViewById(R.id.swap_why_more_toggle), whyDetails, savedInstanceState);
        MoreText.setUp(findViewById(R.id.swap_after_more_toggle), afterDetails, savedInstanceState);
        confirmButton.setOnClickListener(view -> viewModel.confirm());
        backButton.setOnClickListener(view -> finish());
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
        // Which "More about this" parts are open, so a rotation keeps them.
        MoreText.save(outState, whyDetails, afterDetails);
    }

    private void show(SwapReviewViewModel.State state) {
        if (state.done) {
            if (!closing) {
                closing = true;
                // The screen underneath (Home) says that the swap was recorded.
                Confirmations.showOnNextScreen(this, getString(R.string.swap_saved));
                finish();
            }
            return;
        }
        MessageText.show(message, state.message, state.rulesError);

        SwapReviewViewModel.Texts texts = state.texts;
        content.setVisibility(texts != null ? View.VISIBLE : View.GONE);
        if (texts != null) {
            name.setText(texts.name);
            productDetails.setText(texts.productDetails);
            sample.setVisibility(texts.sample ? View.VISIBLE : View.GONE);
            eachWay.setText(texts.eachWay);
            if (texts.diagram != null) {
                SwapDiagram.bind(diagram, texts.diagram);
            }
            whyRules.setText(texts.whyRules);
            facts.setText(texts.facts);
            kitPlace.setText(texts.kitPlace);
            kitBefore.setText(texts.kitBefore);
            kitAfter.setText(texts.kitAfter);
            pantryPlace.setText(texts.pantryPlace);
            pantryBefore.setText(texts.pantryBefore);
            pantryAfter.setText(texts.pantryAfter);
            totals.setContentDescription(texts.totalsSpoken);
            afterKit.setText(texts.afterKit);
            afterPantry.setText(texts.afterPantry);
            afterNewBatch.setVisibility(texts.partMoves ? View.VISIBLE : View.GONE);
        }

        if (state.result != 0) {
            resultText.setText(state.result);
            resultText.setVisibility(View.VISIBLE);
            // "Confirm swap" is in the bar at the bottom, so the household may be far from the
            // end of the screen, where the message appears: it scrolls into view once.
            if (state.result != shownResult) {
                OneShotPreDrawListener.add(scrollView, () -> scrollView.smoothScrollTo(0,
                        Math.max(0, resultText.getTop() - getResources().getDimensionPixelSize(
                                R.dimen.space_large))));
            }
        } else {
            resultText.setVisibility(View.GONE);
        }
        shownResult = state.result;
        // The bar at the bottom holds only "Confirm swap", so it shows exactly when that does.
        bottomBar.setVisibility(state.canConfirm ? View.VISIBLE : View.GONE);
        confirmButton.setEnabled(!state.saving);
        backButton.setEnabled(!state.saving);
    }
}

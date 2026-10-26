package com.example.readyshelf.ui.batch;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.readyshelf.R;
import com.example.readyshelf.data.Product;
import com.example.readyshelf.data.StorageLocation;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.CategoryGuess;
import com.example.readyshelf.domain.DateEntry;
import com.example.readyshelf.domain.DateLabel;
import com.example.readyshelf.domain.ItemCategory;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.PhotoDateChoices;
import com.example.readyshelf.domain.StockChange;
import com.example.readyshelf.ui.common.BatchText;
import com.example.readyshelf.ui.common.CategoryLook;
import com.example.readyshelf.ui.common.Confirmations;
import com.example.readyshelf.ui.common.DisplayNames;
import com.example.readyshelf.ui.common.ScreenSetup;
import com.example.readyshelf.ui.photo.LabelPhotosActivity;
import com.example.readyshelf.ui.photo.PhotoEntryResult;
import com.example.readyshelf.ui.photo.PhotoText;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The form for adding a new batch or editing one. When adding, label photos can fill in parts
 * of it (the Label photos screen), and the household checks everything before saving. Since
 * 2026-10-23 the button that opens label photos is hidden (LABEL_PHOTOS_SHOWN).
 * It also records a bought item from the purchase list as a new batch ("Record bought item").
 * Nothing is saved until the household checks the review and taps "Confirm and save" (or "Save
 * and add another", which then opens an empty form with the same category and location).
 *
 * <p>The kind of item comes first, because it decides what else the form asks: no quantity or
 * unit for a new document or subscription (until 2026-10-22 also a food section for Food). The
 * choices are chips: one tap each. Since 2026-10-22 the label type shows as one line until
 * "Change" is tapped, and "What is printed?" stays folded until it is needed. The ViewModel
 * keeps every choice; the chips only show it and report taps.
 */
public class BatchEditActivity extends AppCompatActivity {

    private static final String EXTRA_BATCH_ID = "com.example.readyshelf.BATCH_ID";
    private static final String EXTRA_PURCHASE_ITEM_ID = "com.example.readyshelf.PURCHASE_ITEM_ID";
    private static final String EXTRA_PRODUCT_ID = "com.example.readyshelf.PRODUCT_ID";
    private static final String EXTRA_KIT_LOCATION_ID = "com.example.readyshelf.KIT_LOCATION_ID";
    private static final String EXTRA_QUANTITY = "com.example.readyshelf.QUANTITY";
    private static final String EXTRA_ANOTHER_CATEGORY = "com.example.readyshelf.ANOTHER_CATEGORY";
    private static final String EXTRA_ANOTHER_LOCATION_ID = "com.example.readyshelf.ANOTHER_LOCATION_ID";

    // The household doesn't use label photos, so "Take label photos" is hidden (the student's
    // request, 2026-10-23). The Label photos screen and its code stay; true shows the button again.
    private static final boolean LABEL_PHOTOS_SHOWN = false;

    /** Opens the form to add a new batch. */
    @NonNull
    public static Intent newAddIntent(@NonNull Context context) {
        return new Intent(context, BatchEditActivity.class);
    }

    /** Opens the form to edit an existing batch. */
    @NonNull
    public static Intent newEditIntent(@NonNull Context context, long batchId) {
        return new Intent(context, BatchEditActivity.class).putExtra(EXTRA_BATCH_ID, batchId);
    }

    /**
     * Opens the form to record a bought item from the purchase list as a new batch.
     * The product is fixed, the amount is filled in, and the printed date is required.
     */
    @NonNull
    public static Intent newPurchaseIntent(@NonNull Context context, long purchaseItemId,
                                           long productId, long kitLocationId, int quantity) {
        return new Intent(context, BatchEditActivity.class)
                .putExtra(EXTRA_PURCHASE_ITEM_ID, purchaseItemId)
                .putExtra(EXTRA_PRODUCT_ID, productId)
                .putExtra(EXTRA_KIT_LOCATION_ID, kitLocationId)
                .putExtra(EXTRA_QUANTITY, quantity);
    }

    /** After "Save and add another": an empty form with the category and location just used. */
    @NonNull
    private static Intent newAnotherIntent(@NonNull Context context, @NonNull ItemCategory category,
                                           long locationId) {
        return newAddIntent(context)
                .putExtra(EXTRA_ANOTHER_CATEGORY, category.name())
                .putExtra(EXTRA_ANOTHER_LOCATION_ID, locationId);
    }

    /**
     * One product in the product list. The list searches its one-line text, and the field shows
     * that text after a choice. The row in the list shows the name, then on the next line the
     * variant, package size, and unit.
     */
    private static final class ProductChoice {
        final long id;
        final String text;
        final String rowText;

        ProductChoice(long id, String text, String rowText) {
            this.id = id;
            this.text = text;
            this.rowText = rowText;
        }

        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }

    /** The rows of the product list. */
    private final class ProductChoiceAdapter extends ArrayAdapter<ProductChoice> {

        ProductChoiceAdapter(@NonNull List<ProductChoice> choices) {
            super(BatchEditActivity.this, R.layout.item_product_choice, choices);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView row = (TextView) super.getView(position, convertView, parent);
            ProductChoice choice = getItem(position);
            if (choice != null) {
                row.setText(choice.rowText);
            }
            // The list can measure a row before it knows its own width. A maximum width makes
            // long text wrap onto more lines instead of being cut off at the right edge.
            int width = parent.getWidth() > 0 ? parent.getWidth() : productLayout.getWidth();
            if (width > 0) {
                row.setMaxWidth(width);
            }
            return row;
        }
    }

    /** Called with the value of the chip that the household tapped. */
    private interface OnChoice {
        void onChoice(@Nullable Object value);
    }

    // Opens the Label photos screen. It is registered every time the screen is made (Android
    // requires that), so its result still arrives after Android closed and reopened the app.
    private final ActivityResultLauncher<Void> labelPhotos =
            registerForActivityResult(new LabelPhotosActivity.Contract(), this::onLabelPhotosDone);

    private BatchEditViewModel viewModel;
    private NestedScrollView scrollView;
    private View formSection;
    private View reviewSection;
    private TextView messageText;
    private TextView sampleNote;
    private TextView purchaseNote;
    // What kind of item
    private ChipGroup categoryChips;
    private TextView categoryNote;
    // Product
    private View existingGroup;
    private View newGroup;
    private TextInputLayout productLayout;
    private MaterialAutoCompleteTextView productField;
    private TextView productFacts;
    private TextView productNoneYet;
    private MaterialButton productAddNew;
    private View productChooseExisting;
    private TextInputLayout nameLayout;
    private EditText nameField;
    private TextInputLayout unitLayout;
    private EditText unitField;
    private ChipGroup unitChips;
    // Where and how many
    private TextView placeHeading;
    private View locationGroup;
    private ChipGroup locationChips;
    private TextView locationError;
    private TextView locationText;
    private View quantityRow;
    private TextInputLayout quantityLayout;
    private EditText quantityField;
    private View quantityLess;
    private View quantityMore;
    private TextView oneItemNote;
    // Date
    private TextView dateHeading;
    private View dateLabelRow;
    private TextView dateLabelText;
    private View dateLabelGroup;
    private ChipGroup dateLabelChips;
    private View dateKindGroup;
    private TextView dateKindHeading;
    private MaterialButton dateKindShow;
    private ChipGroup dateKindChips;
    private Chip dateKindNone;
    private TextView dateNoneNote;
    private TextView dateKindError;
    private View completeGroup;
    private MaterialButton chooseDateButton;
    private TextView dateError;
    private CheckBox dateConfirm;
    private TextInputLayout printedDateLayout;
    private EditText printedDateField;
    private Button monthEndButton;
    private TextView monthEndNote;
    private TextView dateNote;
    private TextView dateReview;
    // Review
    private Button saveButton;
    private TextView reviewText;
    private TextView reviewKit;
    private TextView reviewError;
    private Button reviewConfirm;
    private Button reviewAnother;
    private Button reviewBack;
    // Label photos
    private View scanSection;
    private Button photosButton;
    private TextView scanStatus;
    private TextView scanNamesHeading;
    private LinearLayout scanNames;

    // The photo result whose name buttons are on screen, so they are only rebuilt for a new one.
    @Nullable private PhotoEntryResult shownPhotoResult;
    // The category whose unit choices are on screen, so they are only rebuilt for a new one.
    @Nullable private ItemCategory shownUnitCategory;
    // The Label photos screen is open, so its button waits (a double tap opens it only once).
    private boolean photosOpen;
    // A dialog that is open now. It is closed in onDestroy, so it can't leak when the screen rotates.
    @Nullable private Dialog openDialog;
    // While the review is shown, Back returns to the form instead of closing the screen.
    private OnBackPressedCallback backToForm;
    // On the form, Back asks "Leave without saving?" first, once the household entered something.
    private OnBackPressedCallback askBeforeLeaving;
    private boolean reviewWasShown;
    // The save error on screen (0 for none), so a new one scrolls into view only once.
    @StringRes private int shownSaveError;
    // The batch is saved and the screen is closing, so its save message is left only once.
    private boolean closing;
    // The screen is checking a chip to show the ViewModel's choice. That is not a tap, so the
    // chip listeners ignore it.
    private boolean showingChoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSetup.apply(this, R.layout.activity_batch_edit);
        viewModel = new ViewModelProvider(this).get(BatchEditViewModel.class);
        Intent intent = getIntent();
        long purchaseItemId = intent.getLongExtra(EXTRA_PURCHASE_ITEM_ID, 0);
        String anotherCategory = intent.getStringExtra(EXTRA_ANOTHER_CATEGORY);
        if (purchaseItemId != 0) {
            viewModel.startPurchase(purchaseItemId, intent.getLongExtra(EXTRA_PRODUCT_ID, 0),
                    intent.getLongExtra(EXTRA_KIT_LOCATION_ID, 0), intent.getIntExtra(EXTRA_QUANTITY, 0));
        } else if (anotherCategory != null) {
            viewModel.startAnother(ItemCategory.valueOf(anotherCategory),
                    intent.getLongExtra(EXTRA_ANOTHER_LOCATION_ID, 0));
        } else {
            viewModel.start(intent.getLongExtra(EXTRA_BATCH_ID, 0));
        }
        findViews();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (viewModel.isEditing()) {
            toolbar.setTitle(R.string.title_edit_batch);
            setTitle(R.string.title_edit_batch);
            quantityLayout.setHelperText(getString(R.string.form_quantity_edit_helper));
        } else if (viewModel.isPurchase()) {
            toolbar.setTitle(R.string.title_record_purchase);
            setTitle(R.string.title_record_purchase);
        } else if (savedInstanceState == null) {
            // A new batch starts at 1; − and + change it, or the household types the number.
            quantityField.setText("1");
        }
        backToForm = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.cancelReview();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToForm);
        // Off until the household enters something (updateLeaveQuestion), so an untouched form
        // closes at once. The two callbacks are never on together: one is for the review, the
        // other for the form.
        askBeforeLeaving = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, askBeforeLeaving);
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        setUpCategoryChoices();
        setUpProduct();
        setUpDateChoices();
        if (viewModel.isPurchase()) {
            setUpPurchase(); // after the chips are made, because it turns "No date" off
        }
        setUpQuantity();
        onChoice(locationChips, false, value -> viewModel.setLocationId((Long) value));
        viewModel.getLocations().observe(this, this::showLocations);

        LiveData<BatchInfo> batchToEdit = viewModel.getBatchToEdit();
        if (batchToEdit != null) {
            batchToEdit.observe(this, this::showBatchToEdit);
        }

        watchTypedFields();
        // The typed fields as this screen filled them, once: empty, or the quantity it starts
        // with. An edit fills them later, when its batch has loaded (showBatchToEdit).
        if (!viewModel.isEditing() && viewModel.getTextBaseline() == null) {
            viewModel.setTextBaseline(typedTexts());
        }

        photosButton.setOnClickListener(view -> openLabelPhotos());
        findViewById(R.id.edit_new_location).setOnClickListener(view -> showNewLocationDialog());
        saveButton.setOnClickListener(view -> review());
        reviewConfirm.setOnClickListener(view -> viewModel.confirmSave(false));
        reviewAnother.setOnClickListener(view -> viewModel.confirmSave(true));
        reviewBack.setOnClickListener(view -> viewModel.cancelReview());

        viewModel.getState().observe(this, this::show);
        viewModel.getLocationMessage().observe(this, message -> {
            if (message != null) {
                Confirmations.show(this, getString(message));
                viewModel.locationMessageShown();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (openDialog != null) {
            openDialog.dismiss();
        }
        super.onDestroy();
    }

    private void findViews() {
        scrollView = findViewById(R.id.edit_scroll);
        formSection = findViewById(R.id.edit_form);
        reviewSection = findViewById(R.id.edit_review);
        messageText = findViewById(R.id.edit_message);
        sampleNote = findViewById(R.id.edit_sample_note);
        purchaseNote = findViewById(R.id.edit_purchase_note);
        categoryChips = findViewById(R.id.edit_category_chips);
        categoryNote = findViewById(R.id.edit_category_note);
        existingGroup = findViewById(R.id.edit_existing_group);
        newGroup = findViewById(R.id.edit_new_group);
        productLayout = findViewById(R.id.edit_product_layout);
        productField = findViewById(R.id.edit_product);
        productFacts = findViewById(R.id.edit_product_facts);
        productNoneYet = findViewById(R.id.edit_product_none_yet);
        productAddNew = findViewById(R.id.edit_product_add_new);
        productChooseExisting = findViewById(R.id.edit_product_choose_existing);
        nameLayout = findViewById(R.id.edit_name_layout);
        nameField = findViewById(R.id.edit_name);
        unitLayout = findViewById(R.id.edit_unit_layout);
        unitField = findViewById(R.id.edit_unit);
        unitChips = findViewById(R.id.edit_unit_chips);
        placeHeading = findViewById(R.id.edit_place_heading);
        locationGroup = findViewById(R.id.edit_location_group);
        locationChips = findViewById(R.id.edit_location_chips);
        locationError = findViewById(R.id.edit_location_error);
        locationText = findViewById(R.id.edit_location_text);
        quantityRow = findViewById(R.id.edit_quantity_row);
        quantityLayout = findViewById(R.id.edit_quantity_layout);
        quantityField = findViewById(R.id.edit_quantity);
        quantityLess = findViewById(R.id.edit_quantity_less);
        quantityMore = findViewById(R.id.edit_quantity_more);
        oneItemNote = findViewById(R.id.edit_one_item_note);
        dateHeading = findViewById(R.id.edit_date_heading);
        dateLabelRow = findViewById(R.id.edit_date_label_row);
        dateLabelText = findViewById(R.id.edit_date_label_text);
        dateLabelGroup = findViewById(R.id.edit_date_label_group);
        dateLabelChips = findViewById(R.id.edit_date_label_chips);
        dateKindGroup = findViewById(R.id.edit_date_kind_group);
        dateKindHeading = findViewById(R.id.edit_date_kind_heading);
        dateKindShow = findViewById(R.id.edit_date_kind_show);
        dateKindChips = findViewById(R.id.edit_date_kind_chips);
        dateNoneNote = findViewById(R.id.edit_date_none_note);
        dateKindError = findViewById(R.id.edit_date_kind_error);
        completeGroup = findViewById(R.id.edit_complete_group);
        chooseDateButton = findViewById(R.id.edit_choose_date);
        dateError = findViewById(R.id.edit_date_error);
        dateConfirm = findViewById(R.id.edit_date_confirm);
        printedDateLayout = findViewById(R.id.edit_printed_date_layout);
        printedDateField = findViewById(R.id.edit_printed_date);
        monthEndButton = findViewById(R.id.edit_use_month_end);
        monthEndNote = findViewById(R.id.edit_month_end_note);
        dateNote = findViewById(R.id.edit_date_note);
        dateReview = findViewById(R.id.edit_date_review);
        saveButton = findViewById(R.id.edit_save);
        reviewText = findViewById(R.id.edit_review_text);
        reviewKit = findViewById(R.id.edit_review_kit);
        reviewError = findViewById(R.id.edit_review_error);
        reviewConfirm = findViewById(R.id.edit_review_confirm);
        reviewAnother = findViewById(R.id.edit_review_another);
        reviewBack = findViewById(R.id.edit_review_back);
        scanSection = findViewById(R.id.scan_section);
        photosButton = findViewById(R.id.scan_take_photos);
        scanStatus = findViewById(R.id.scan_status);
        scanNamesHeading = findViewById(R.id.scan_names_heading);
        scanNames = findViewById(R.id.scan_names);
    }

    // ----- Chips: one-tap choices -----

    /**
     * Adds a chip to a group of choices. Its tag is the value it stands for. Like Inventory's
     * filters, a chosen chip shows a check mark, so the choice is not shown by color alone.
     */
    @NonNull
    private Chip addChoice(@NonNull ChipGroup group, @NonNull CharSequence label, @Nullable Object value) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(value);
        // The ViewModel keeps the choice. A chip that restored its own old state could disagree.
        chip.setSaveEnabled(false);
        group.addView(chip);
        return chip;
    }

    /**
     * Calls {@code listener} with the value of a chip the household taps in {@code group}. Such a
     * tap is something the household entered, so Back asks before leaving (touched).
     */
    private void onChoice(@NonNull ChipGroup group, @NonNull OnChoice listener) {
        onChoice(group, true, listener);
    }

    /**
     * Like {@link #onChoice(ChipGroup, OnChoice)}. With {@code countsAsInput} false, a tap alone
     * doesn't make Back ask: on a new batch the category and the location are one tap to choose
     * again, and "Save and add another" fills them in by itself.
     */
    private void onChoice(@NonNull ChipGroup group, boolean countsAsInput,
                          @NonNull OnChoice listener) {
        group.setOnCheckedStateChangeListener((chips, checkedIds) -> {
            if (!showingChoice && !checkedIds.isEmpty()) {
                if (countsAsInput) {
                    touched(); // a tap, not the screen showing the ViewModel's choice
                }
                listener.onChoice(chips.findViewById(checkedIds.get(0)).getTag());
            }
        });
    }

    /** Checks the chip that stands for {@code value}, to show the ViewModel's choice. */
    private void check(@NonNull ChipGroup group, @Nullable Object value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View chip = group.getChildAt(i);
            if (Objects.equals(chip.getTag(), value)) {
                if (group.getCheckedChipId() != chip.getId()) {
                    showingChoice = true;
                    group.check(chip.getId());
                    showingChoice = false;
                }
                return;
            }
        }
    }

    // ----- Setting up the choices -----

    /**
     * One chip per category, each with its icon (show() swaps the chosen one's icon for the
     * check mark). The icon only helps: the word says it.
     */
    private void setUpCategoryChoices() {
        for (ItemCategory category : ItemCategory.values()) {
            Chip chip = addChoice(categoryChips, getString(DisplayNames.category(category)), category);
            chip.setChipIconResource(CategoryLook.icon(category));
        }
        // On an edit, a new category is a change to the saved batch, so it counts.
        onChoice(categoryChips, viewModel.isEditing(),
                value -> viewModel.setCategory((ItemCategory) value));
    }

    private void setUpProduct() {
        // The ViewModel keeps the search text, so the field doesn't save its own copy.
        productField.setSaveEnabled(false);
        productField.addTextChangedListener(afterTextChanged(text -> {
            // The screen only ever copies the ViewModel's text here, so other text was typed.
            if (!text.equals(viewModel.getProductSearchText())) {
                touched();
            }
            viewModel.setProductSearchText(text);
            productLayout.setError(null);
        }));
        productField.setOnItemClickListener((parent, view, position, id) -> {
            ProductChoice choice = (ProductChoice) parent.getItemAtPosition(position);
            touched();
            viewModel.selectProduct(choice.id);
        });
        viewModel.getProducts().observe(this, products -> showProducts());

        productAddNew.setOnClickListener(view -> addNewProduct());
        productChooseExisting.setOnClickListener(view -> {
            touched();
            viewModel.setNewProduct(false);
        });
        unitField.addTextChangedListener(afterTextChanged(text -> {
            updateQuantityHint();
            showQuantitySteps();
        }));
    }

    /** The − and + buttons beside the quantity. Typing a number still works. */
    private void setUpQuantity() {
        quantityLess.setOnClickListener(view -> stepQuantity(-1));
        quantityMore.setOnClickListener(view -> stepQuantity(1));
        quantityField.addTextChangedListener(afterTextChanged(text -> showQuantitySteps()));
        showQuantitySteps();
    }

    private void setUpDateChoices() {
        for (DateLabel label : DateLabel.values()) {
            addChoice(dateLabelChips, getString(DisplayNames.dateLabel(label)), label);
        }
        onChoice(dateLabelChips, value -> viewModel.setDateLabel((DateLabel) value));

        addChoice(dateKindChips, getString(R.string.form_date_complete), DateEntry.Kind.COMPLETE);
        addChoice(dateKindChips, getString(R.string.form_date_incomplete), DateEntry.Kind.INCOMPLETE);
        dateKindNone = addChoice(dateKindChips, getString(R.string.form_date_none), DateEntry.Kind.NONE);
        onChoice(dateKindChips, value -> viewModel.setDateKind((DateEntry.Kind) value));

        // The two folded parts. Opening one changes no answer, so Back doesn't ask about it.
        // The button that opens a part goes away, so TalkBack says which part appeared (its
        // pane title) instead.
        ViewCompat.setAccessibilityPaneTitle(dateLabelGroup, getString(R.string.form_date_label));
        ViewCompat.setAccessibilityPaneTitle(dateKindGroup, getString(R.string.form_date_kind));
        findViewById(R.id.edit_date_label_change).setOnClickListener(view -> viewModel.openDateLabel());
        dateKindShow.setOnClickListener(view -> {
            viewModel.openDateKind();
            reveal(dateKindHeading); // it opens above the button, so the screen scrolls up to it
        });

        chooseDateButton.setOnClickListener(view -> showDatePicker());
        dateConfirm.setOnCheckedChangeListener((button, checked) -> {
            // The screen only ever shows the ViewModel's tick, so a different one is a tap.
            if (checked != viewModel.isDateConfirmed()) {
                touched();
            }
            viewModel.setDateConfirmed(checked);
        });

        // The ViewModel keeps the printed text, because a label photo can fill it in.
        printedDateField.setSaveEnabled(false);
        printedDateField.addTextChangedListener(afterTextChanged(text -> {
            if (!text.equals(viewModel.getPrintedDateText())) {
                touched(); // typed: the screen only copies the ViewModel's text here
            }
            viewModel.setPrintedDateText(text);
            printedDateLayout.setError(null);
        }));
        monthEndButton.setOnClickListener(view -> useMonthEnd());
    }

    /**
     * Recording a bought item: the product is the purchase item's product, so it can't be
     * changed, and "No date" is turned off, because a bought item needs its printed date.
     */
    private void setUpPurchase() {
        productLayout.setEnabled(false);
        dateKindNone.setEnabled(false);
        dateNoneNote.setVisibility(View.VISIBLE);
        Integer quantity = viewModel.takePurchaseQuantity();
        if (quantity != null) {
            quantityField.setText(String.valueOf(quantity));
        }
    }

    // ----- Showing data -----

    /** Rebuilds the product list. Each choice shows the variant, package size, and unit. */
    private void showProducts() {
        List<ProductChoice> choices = new ArrayList<>();
        for (Product product : viewModel.choosableProducts()) {
            String details = BatchText.productChoiceDetails(this, product.variant,
                    product.packageSize, product.unit);
            choices.add(new ProductChoice(product.id, viewModel.choiceText(product),
                    product.name + System.lineSeparator() + details));
        }
        productField.setAdapter(new ProductChoiceAdapter(choices));
    }

    /**
     * Rebuilds the location chips when locations change (for example, after "New location"),
     * and checks the location the ViewModel chooses.
     */
    private void showLocations(List<StorageLocation> locations) {
        int position = viewModel.chooseLocationPosition(locations);
        showingChoice = true;
        locationChips.removeAllViews();
        for (StorageLocation location : locations) {
            addChoice(locationChips, location.name, location.id);
        }
        showingChoice = false;
        if (position >= 0) {
            check(locationChips, locations.get(position).id);
            locationError.setVisibility(View.GONE);
        }
    }

    /** Fills the form from the batch being edited (once), or explains that it is gone. */
    private void showBatchToEdit(@Nullable BatchInfo batch) {
        if (batch == null) {
            messageText.setText(R.string.form_missing);
            messageText.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
            reviewConfirm.setEnabled(false);
            return;
        }
        if (viewModel.takeLoadedBatch(batch)) {
            quantityField.setText(String.valueOf(batch.getQuantity()));
            // The typed fields as the edit starts, so only later typing counts as a change.
            viewModel.setTextBaseline(typedTexts());
        }
        locationText.setText(getString(R.string.form_location_edit, batch.getLocationName()));
        sampleNote.setVisibility(viewModel.isSample() ? View.VISIBLE : View.GONE);
    }

    /** Shows the ViewModel's choices, notes, label photos, and review. */
    private void show(BatchEditViewModel.State state) {
        boolean editing = viewModel.isEditing();
        formSection.setVisibility(state.reviewing ? View.GONE : View.VISIBLE);
        reviewSection.setVisibility(state.reviewing ? View.VISIBLE : View.GONE);
        // The bar at the bottom always shows the step's main button.
        saveButton.setVisibility(state.reviewing ? View.GONE : View.VISIBLE);
        reviewConfirm.setVisibility(state.reviewing ? View.VISIBLE : View.GONE);
        // Label photos are only for adding a new batch: not when editing, and not for a bought
        // item from the purchase list, whose product is fixed. Since 2026-10-23 they are hidden.
        scanSection.setVisibility(LABEL_PHOTOS_SHOWN && !editing && !viewModel.isPurchase()
                && !state.reviewing ? View.VISIBLE : View.GONE);
        backToForm.setEnabled(state.reviewing);
        if (state.reviewing != reviewWasShown) {
            reviewWasShown = state.reviewing;
            scrollView.scrollTo(0, 0);
        }

        // The ViewModel owns the choices; showing the same value again changes nothing.
        ItemCategory category = viewModel.getCategory();
        check(categoryChips, category);
        // A chip shows its check mark only without its icon, so on the chosen category the
        // check mark takes the icon's place, and the choice is not shown by color alone.
        for (int i = 0; i < categoryChips.getChildCount(); i++) {
            Chip chip = (Chip) categoryChips.getChildAt(i);
            chip.setChipIconVisible(!chip.isChecked());
        }
        if (state.categoryCue != null) {
            categoryNote.setText(getString(R.string.scan_note_category, state.categoryCue));
            categoryNote.setVisibility(View.VISIBLE);
        } else {
            categoryNote.setVisibility(View.GONE);
        }
        showProductChoices();
        showPlaceAndQuantity();
        locationGroup.setVisibility(editing ? View.GONE : View.VISIBLE);
        locationText.setVisibility(editing ? View.VISIBLE : View.GONE);
        purchaseNote.setText(state.purchaseNote);
        purchaseNote.setVisibility(state.purchaseNote == null ? View.GONE : View.VISIBLE);
        showDateEntry(state);
        showReview(state);
        showPhotos(state);
        if (state.done && !closing) {
            // Saved: the screen closes, and the next screen says what was saved. After "Save
            // and add another", that is an empty form for the next batch.
            closing = true;
            Confirmations.showOnNextScreen(this, getString(state.nothingChanged
                    ? R.string.confirm_no_changes
                    : editing ? R.string.confirm_batch_changed : R.string.confirm_batch_added));
            if (state.addAnother) {
                startActivity(newAnotherIntent(this, category, viewModel.getLocationId()));
            }
            finish();
        }
        updateLeaveQuestion();
    }

    // ----- Leaving without saving -----

    /** The household changed something by hand, so Back asks before the form is left. */
    private void touched() {
        viewModel.markTouched();
        updateLeaveQuestion();
    }

    /**
     * Back asks first (confirmLeave) only on the form (not the review, where Back returns to the
     * form), and only when leaving would lose something the household entered.
     */
    private void updateLeaveQuestion() {
        if (askBeforeLeaving == null) {
            return; // the screen is still being set up
        }
        BatchEditViewModel.State state = viewModel.getState().getValue();
        boolean reviewing = state != null && state.reviewing;
        askBeforeLeaving.setEnabled(!reviewing && !closing
                && viewModel.hasUnsavedInput(typedTexts()));
    }

    /** The typed fields, always in the same order, to compare with how the screen filled them. */
    @NonNull
    private ArrayList<String> typedTexts() {
        ArrayList<String> texts = new ArrayList<>();
        for (EditText field : new EditText[] {nameField, unitField, quantityField}) {
            texts.add(textOf(field, true));
        }
        return texts;
    }

    /** Typing in a field can turn the question on or off again. */
    private void watchTypedFields() {
        for (EditText field : new EditText[] {nameField, unitField, quantityField}) {
            field.addTextChangedListener(afterTextChanged(text -> updateLeaveQuestion()));
        }
    }

    /**
     * Back on the form after the household entered something: asks first, so it isn't lost by
     * accident (the same question as Settings). "Leave" closes the form and saves nothing;
     * "Keep editing" stays here.
     */
    private void confirmLeave() {
        openDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_leave_title)
                .setMessage(R.string.form_leave_message)
                .setPositiveButton(R.string.settings_leave_confirm, (dialog, which) -> finish())
                .setNegativeButton(R.string.settings_leave_keep_editing, null)
                .show();
    }

    private void showProductChoices() {
        boolean newProduct = viewModel.isNewProduct();
        existingGroup.setVisibility(newProduct ? View.GONE : View.VISIBLE);
        newGroup.setVisibility(newProduct ? View.VISIBLE : View.GONE);

        String searchText = viewModel.getProductSearchText();
        if (!searchText.equals(textOf(productField, false))) {
            productField.setText(searchText, false); // false: don't open the list
        }
        Product product = viewModel.selectedProduct();
        if (product != null) {
            productFacts.setText(BatchText.productFacts(this, product.variant, product.packageSize,
                    product.unit));
            productFacts.setVisibility(View.VISIBLE);
        } else {
            productFacts.setVisibility(View.GONE);
        }
        boolean noProducts = viewModel.areProductsLoaded() && viewModel.choosableProducts().isEmpty();
        productNoneYet.setVisibility(noProducts ? View.VISIBLE : View.GONE);

        // "New product", or "Add “Rice” as a new product" while the typed words are not one of
        // the household's products. A bought item's product is fixed, so neither is offered.
        boolean purchase = viewModel.isPurchase();
        String typed = searchText.trim();
        productAddNew.setVisibility(purchase ? View.GONE : View.VISIBLE);
        productAddNew.setText(typed.isEmpty() || product != null ? getString(R.string.form_product_new)
                : getString(R.string.form_product_add_new, typed));
        productChooseExisting.setVisibility(purchase || viewModel.choosableProducts().isEmpty()
                ? View.GONE : View.VISIBLE);

        // A new document or subscription gets its unit by itself.
        boolean asksUnit = !viewModel.isOneItem();
        unitLayout.setVisibility(asksUnit ? View.VISIBLE : View.GONE);
        unitChips.setVisibility(asksUnit ? View.VISIBLE : View.GONE);
        showUnitChoices(viewModel.getCategory());
        updateQuantityHint();
    }

    /** The one-tap units for the category, for example "cans" and "boxes" for Food. */
    private void showUnitChoices(@NonNull ItemCategory category) {
        if (category == shownUnitCategory) {
            return;
        }
        shownUnitCategory = category;
        unitChips.removeAllViews();
        for (String unit : getResources().getStringArray(CategoryDefaults.unitSuggestions(category))) {
            Chip chip = addChoice(unitChips, unit, unit);
            // Not a lasting choice: a tap only types the unit into the field above.
            chip.setCheckable(false);
            chip.setOnClickListener(view -> {
                unitField.setText(unit);
                unitField.setSelection(unitField.length());
                unitLayout.setError(null);
            });
        }
    }

    /**
     * The household chose "New product" (or "Add “Rice” as a new product"): the typed words
     * become the new product's name, so nothing is typed twice.
     */
    private void addNewProduct() {
        touched();
        String typed = textOf(productField, true);
        if (!typed.isEmpty() && viewModel.selectedProduct() == null) {
            nameField.setText(typed);
            nameField.setSelection(nameField.length());
        }
        nameLayout.setError(null);
        viewModel.setNewProduct(true);
    }

    /**
     * The heading, and the quantity row or (for a new document or subscription, which is one
     * item) the note that says what is saved instead.
     */
    private void showPlaceAndQuantity() {
        boolean oneItem = viewModel.isOneItem();
        placeHeading.setText(oneItem ? R.string.form_place_heading_one_item
                : R.string.form_place_heading);
        quantityRow.setVisibility(oneItem ? View.GONE : View.VISIBLE);
        oneItemNote.setVisibility(oneItem ? View.VISIBLE : View.GONE);
        if (oneItem) {
            String unit = viewModel.isNewProduct() || viewModel.selectedProduct() == null
                    ? viewModel.oneItemUnit() : viewModel.selectedUnit();
            oneItemNote.setText(getString(R.string.form_one_item_note, BatchText.amount(this, 1, unit)));
        }
    }

    /** "Quantity (cans)": the unit of the chosen or new product. */
    private void updateQuantityHint() {
        String unit = currentUnit();
        quantityLayout.setHint(unit.isEmpty() ? getString(R.string.form_quantity)
                : getString(R.string.form_quantity_with_unit, unit));
    }

    /** The unit of the chosen product, or the unit typed for a new one. */
    @NonNull
    private String currentUnit() {
        return viewModel.isNewProduct() ? textOf(unitField, true) : viewModel.selectedUnit();
    }

    /** − or + was tapped: one less or one more, from 0 (1 for a bought item) to 9999. */
    private void stepQuantity(int step) {
        Integer now = StockChange.parseQuantity(textOf(quantityField, true));
        int next = Math.max(smallestQuantity(),
                Math.min(StockChange.MAX_QUANTITY, (now == null ? 0 : now) + step));
        quantityField.setText(String.valueOf(next));
        quantityField.setSelection(quantityField.length());
        quantityLayout.setError(null);
    }

    private int smallestQuantity() {
        return viewModel.isPurchase() ? 1 : 0;
    }

    /**
     * The − and + buttons stop at the smallest and largest amounts. TalkBack hears the new
     * amount on the button after a tap ("3 cans"), because it is the button's state.
     */
    private void showQuantitySteps() {
        Integer now = StockChange.parseQuantity(textOf(quantityField, true));
        quantityLess.setEnabled(now != null && now > smallestQuantity());
        quantityMore.setEnabled(now == null || now < StockChange.MAX_QUANTITY);
        String amount = now == null ? null : BatchText.amount(this, now, currentUnit());
        ViewCompat.setStateDescription(quantityLess, amount);
        ViewCompat.setStateDescription(quantityMore, amount);
    }

    private void showDateEntry(BatchEditViewModel.State state) {
        // A document or subscription has no package, so its words leave "package" out.
        boolean oneItemCategory = CategoryDefaults.isOneItem(viewModel.getCategory());
        dateHeading.setText(oneItemCategory
                ? R.string.form_date_heading_one_item : R.string.form_date_heading);
        // The label type: one line with "Change", or the chips once "Change" was tapped.
        DateLabel label = viewModel.getDateLabel();
        dateLabelText.setText(getString(R.string.form_date_label_now,
                getString(DisplayNames.dateLabel(label))));
        boolean labelOpen = viewModel.isDateLabelOpen();
        dateLabelRow.setVisibility(labelOpen ? View.GONE : View.VISIBLE);
        dateLabelGroup.setVisibility(labelOpen ? View.VISIBLE : View.GONE);
        check(dateLabelChips, label);
        // "What is printed?", or the button that opens it.
        boolean kindOpen = viewModel.isDateKindOpen();
        dateKindGroup.setVisibility(kindOpen ? View.VISIBLE : View.GONE);
        dateKindShow.setVisibility(kindOpen ? View.GONE : View.VISIBLE);
        dateKindShow.setText(oneItemCategory
                ? R.string.form_date_kind_show_one_item : R.string.form_date_kind_show);
        DateEntry.Kind kind = viewModel.getDateKind();
        check(dateKindChips, kind);
        completeGroup.setVisibility(kind == DateEntry.Kind.COMPLETE ? View.VISIBLE : View.GONE);
        printedDateLayout.setVisibility(kind == DateEntry.Kind.INCOMPLETE ? View.VISIBLE : View.GONE);

        LocalDate date = viewModel.getDate();
        if (date == null) {
            chooseDateButton.setText(R.string.form_choose_date);
            chooseDateButton.setContentDescription(null);
        } else {
            String shown = BatchText.date(date);
            chooseDateButton.setText(shown);
            chooseDateButton.setContentDescription(getString(R.string.form_date_chosen_description, shown));
            dateError.setVisibility(View.GONE);
        }
        dateConfirm.setEnabled(date != null);
        dateConfirm.setChecked(date != null && viewModel.isDateConfirmed());

        String printed = viewModel.getPrintedDateText();
        if (!printed.equals(textOf(printedDateField, false))) {
            printedDateField.setText(printed);
        }
        LocalDate monthEnd = viewModel.getMonthEndChoice();
        if (monthEnd != null) {
            monthEndButton.setText(getString(R.string.form_use_month_end, BatchText.date(monthEnd)));
            monthEndButton.setVisibility(View.VISIBLE);
        } else {
            monthEndButton.setVisibility(View.GONE);
        }
        if (state.monthEndFrom != null) {
            monthEndNote.setText(getString(R.string.form_month_end_note, state.monthEndFrom));
            monthEndNote.setVisibility(View.VISIBLE);
        } else {
            monthEndNote.setVisibility(View.GONE);
        }

        if (state.dateFromPhoto) {
            dateNote.setText(state.dateMonthOnly ? R.string.scan_note_date_month_only
                    : state.dateAsPrinted ? R.string.scan_note_date_as_printed : R.string.scan_note_date);
            dateNote.setVisibility(View.VISIBLE);
        } else {
            dateNote.setVisibility(View.GONE);
        }
        String review = viewModel.getDateReviewText();
        dateReview.setText(review);
        dateReview.setVisibility(review == null ? View.GONE : View.VISIBLE);
    }

    private void showReview(BatchEditViewModel.State state) {
        reviewText.setText(state.reviewText);
        reviewKit.setText(state.kitWarning);
        reviewKit.setVisibility(state.kitWarning == null ? View.GONE : View.VISIBLE);
        if (state.saveError != 0) {
            reviewError.setText(state.saveError);
            reviewError.setVisibility(View.VISIBLE);
            // "Confirm and save" is in the bar at the bottom, so the household may be far from
            // the end of the review, where the error appears: it scrolls into view once.
            if (state.saveError != shownSaveError) {
                // After the next layout, when the error has its place on screen.
                OneShotPreDrawListener.add(scrollView, () -> reveal(reviewError));
            }
        } else {
            reviewError.setVisibility(View.GONE);
        }
        shownSaveError = state.saveError;
        boolean batchMissing = viewModel.isEditing() && messageText.getVisibility() == View.VISIBLE;
        reviewConfirm.setEnabled(!state.saving && !batchMissing);
        reviewAnother.setVisibility(viewModel.canAddAnother() ? View.VISIBLE : View.GONE);
        reviewAnother.setEnabled(!state.saving);
        reviewBack.setEnabled(!state.saving);
    }

    // ----- Checking the form -----

    /**
     * Checks every field. Each problem is shown next to its field, and the screen scrolls to
     * the first one. When everything is fine, the review is shown.
     */
    private void review() {
        clearErrors();
        View firstProblem = null;
        boolean oneItem = viewModel.isOneItem();

        String name = textOf(nameField, true);
        String unit = oneItem ? viewModel.oneItemUnit() : textOf(unitField, true);
        if (viewModel.isNewProduct()) {
            if (name.isEmpty()) {
                nameLayout.setError(getString(R.string.error_product_name_required));
                firstProblem = nameField;
            } else if (viewModel.findSameProduct(name, unit) != null) {
                nameLayout.setError(getString(R.string.error_product_exists));
                firstProblem = nameField;
            }
            if (unit.isEmpty()) {
                unitLayout.setError(getString(R.string.error_unit_required));
                firstProblem = firstOf(firstProblem, unitField);
            }
        } else if (viewModel.selectedProduct() == null) {
            productLayout.setError(getString(viewModel.choosableProducts().isEmpty()
                    ? R.string.error_product_none_yet : R.string.error_product_required));
            firstProblem = productField;
        }

        // A new document or subscription is one item, so it has no quantity field.
        Integer quantity = oneItem ? Integer.valueOf(1)
                : StockChange.parseQuantity(textOf(quantityField, true));
        boolean purchase = viewModel.isPurchase();
        if (quantity == null || (purchase && quantity < 1)) {
            quantityLayout.setError(getString(purchase ? R.string.error_purchase_quantity
                    : R.string.error_quantity));
            firstProblem = firstOf(firstProblem, quantityField);
        }
        if (!viewModel.isEditing() && viewModel.getLocationId() == 0) {
            locationError.setVisibility(View.VISIBLE);
            firstProblem = firstOf(firstProblem, locationError);
        }
        DateEntry.Kind kind = viewModel.getDateKind();
        if (kind == DateEntry.Kind.COMPLETE && viewModel.getDate() == null) {
            // "Choose a date, or another answer above.": the other answers open above the
            // date button, and the screen scrolls to them, so all three are in view.
            viewModel.openDateKind();
            dateError.setVisibility(View.VISIBLE);
            firstProblem = firstOf(firstProblem, dateKindGroup);
        } else if (kind == DateEntry.Kind.INCOMPLETE && viewModel.getPrintedDateText().trim().isEmpty()) {
            printedDateLayout.setError(getString(purchase ? R.string.error_purchase_date_required
                    : R.string.error_printed_date_required));
            firstProblem = firstOf(firstProblem, printedDateField);
        } else if (kind == DateEntry.Kind.NONE && purchase) {
            // "No date" is turned off for a bought item; this is only a backstop.
            dateKindError.setVisibility(View.VISIBLE);
            firstProblem = firstOf(firstProblem, dateKindError);
        }

        if (firstProblem != null) {
            reveal(firstProblem);
            return;
        }
        // "Review and save" is in the bar at the bottom, so a field can still have the keyboard
        // open. The keyboard closes, so it doesn't cover the review.
        hideKeyboard();
        viewModel.startReview(new BatchEditViewModel.TypedInput(name, unit, quantity));
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(scrollView.getWindowToken(), 0);
        }
    }

    private void clearErrors() {
        productLayout.setError(null);
        nameLayout.setError(null);
        unitLayout.setError(null);
        quantityLayout.setError(null);
        printedDateLayout.setError(null);
        locationError.setVisibility(View.GONE);
        dateError.setVisibility(View.GONE);
        dateKindError.setVisibility(View.GONE);
    }

    @NonNull
    private static View firstOf(@Nullable View firstSoFar, @NonNull View next) {
        return firstSoFar != null ? firstSoFar : next;
    }

    /** Moves to a field with a problem: a text field gets the focus, other views scroll into view. */
    private void reveal(@NonNull View view) {
        if (view instanceof EditText) {
            view.requestFocus();
            return;
        }
        scrollView.post(() -> {
            Rect area = new Rect();
            view.getDrawingRect(area);
            scrollView.offsetDescendantRectToMyCoords(view, area);
            int margin = getResources().getDimensionPixelSize(R.dimen.space_large) * 3;
            scrollView.smoothScrollTo(0, Math.max(0, area.top - margin));
        });
    }

    // ----- Label photos -----

    /** Opens the Label photos screen. Its button waits until the screen closes. */
    private void openLabelPhotos() {
        photosOpen = true;
        photosButton.setEnabled(false);
        labelPhotos.launch(null);
    }

    /** The Label photos screen closed. Null means the household cancelled: nothing changes. */
    private void onLabelPhotosDone(@Nullable PhotoEntryResult result) {
        photosOpen = false;
        photosButton.setEnabled(true);
        if (result != null) {
            touched();
            viewModel.applyPhotoResult(result);
        }
    }

    private void showPhotos(BatchEditViewModel.State state) {
        photosButton.setEnabled(!photosOpen);
        PhotoEntryResult result = state.photoResult;
        String status = result == null ? "" : photoStatusText(state, result);
        scanStatus.setText(status);
        scanStatus.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
        if (result != shownPhotoResult) {
            shownPhotoResult = result;
            showNameChoices(result == null ? new ArrayList<>() : result.getNameLines());
        }
    }

    /** One status line that says what the label photos gave the form. */
    @NonNull
    private String photoStatusText(BatchEditViewModel.State state, @NonNull PhotoEntryResult result) {
        List<String> sentences = new ArrayList<>();
        PhotoDateChoices.Choice date = result.getDate();
        if (date == null) {
            sentences.add(getString(R.string.scan_date_not_chosen));
        } else if (state.dateFromPhoto) { // not after the household changed the date by hand
            sentences.add(getString(date.getLabel() == null ? R.string.scan_date_filled_no_label
                    : R.string.scan_date_filled, PhotoText.date(this, date)));
        }
        CategoryGuess guess = result.getCategoryGuess();
        if (guess == null) {
            sentences.add(getString(R.string.scan_category_none));
        } else {
            String category = getString(DisplayNames.category(guess.getCategory()));
            sentences.add(getString(state.photoCategoryApplied ? R.string.scan_category_set
                    : R.string.scan_category_kept, category, guess.getCue()));
        }
        if (!result.getNameLines().isEmpty()) {
            sentences.add(getString(R.string.scan_names_hint));
        }
        return String.join(" ", sentences);
    }

    private void showNameChoices(@NonNull List<String> lines) {
        scanNames.removeAllViews();
        for (String line : lines) {
            String text = getString(R.string.scan_use_name, line);
            scanNames.addView(choiceButton(text, view -> useScannedName(line)));
        }
        scanNamesHeading.setVisibility(lines.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * A line from the photo was tapped. If the household has products with that name, the
     * product list is searched for it, so the right variant can be chosen. Otherwise the line
     * becomes the name of a new product.
     */
    private void useScannedName(@NonNull String line) {
        touched();
        String existingName = viewModel.existingProductName(line);
        if (existingName != null) {
            viewModel.setNewProduct(false);
            productField.requestFocus();
            productField.setText(existingName); // searches, so the matching products appear
            productLayout.setError(null);
        } else {
            viewModel.setNewProduct(true);
            nameField.setText(line);
            nameLayout.setError(null);
        }
    }

    /** A full-width outlined button whose text wraps onto more lines when it is long. */
    @NonNull
    private MaterialButton choiceButton(@NonNull String text, @NonNull View.OnClickListener onClick) {
        MaterialButton button = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        button.setMaxLines(6);
        button.setOnClickListener(onClick);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        size.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                getResources().getDisplayMetrics());
        button.setLayoutParams(size);
        return button;
    }

    // ----- Date picker, last day of the month, and new location -----

    /**
     * "Use the last day of the month" was tapped. The keyboard closes first, because the field
     * it was typing into is hidden now. TalkBack reads the new date (a live region).
     */
    private void useMonthEnd() {
        hideKeyboard();
        touched();
        viewModel.useMonthEnd();
    }

    /** A date picker with no earliest date, so old stock with a past date can be recorded. */
    private void showDatePicker() {
        LocalDate shown = viewModel.getDate() != null ? viewModel.getDate() : LocalDate.now();
        // DatePickerDialog counts months from 0 (January is 0). LocalDate counts from 1.
        DatePickerDialog dialog = new DatePickerDialog(this,
                (picker, year, month, dayOfMonth) -> {
                    touched();
                    viewModel.setDate(LocalDate.of(year, month + 1, dayOfMonth));
                },
                shown.getYear(), shown.getMonthValue() - 1, shown.getDayOfMonth());
        openDialog = dialog;
        dialog.show();
    }

    // A dialog's view has no parent yet, so inflating with a null parent is correct here.
    @SuppressLint("InflateParams")
    private void showNewLocationDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_new_location, null);
        TextInputLayout newNameLayout = content.findViewById(R.id.new_location_name_layout);
        EditText newNameField = content.findViewById(R.id.new_location_name);
        RadioGroup typeGroup = content.findViewById(R.id.new_location_type);
        // TalkBack reads the question with each answer: "Type: Pantry".
        for (int i = 0; i < typeGroup.getChildCount(); i++) {
            TextView type = (TextView) typeGroup.getChildAt(i);
            type.setContentDescription(getString(R.string.new_location_type_answer, type.getText()));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_location_title)
                .setView(content)
                // The Add button is set up below, so a mistake keeps the dialog open.
                .setPositiveButton(R.string.new_location_add, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(shownDialog -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = textOf(newNameField, true);
                    if (name.isEmpty()) {
                        newNameLayout.setError(getString(R.string.error_name_required));
                        newNameField.requestFocus();
                    } else if (viewModel.isLocationNameTaken(name)) {
                        newNameLayout.setError(getString(R.string.error_location_exists));
                        newNameField.requestFocus();
                    } else {
                        // The location is saved now; choosing it, like any location, isn't
                        // something Back asks about.
                        viewModel.addLocation(name, typeFor(typeGroup.getCheckedRadioButtonId()));
                        dialog.dismiss();
                    }
                }));
        openDialog = dialog;
        dialog.show();
    }

    /** The location type for a radio button. (R.id values can't be switch cases with AGP 9.) */
    private static LocationType typeFor(int radioButtonId) {
        if (radioButtonId == R.id.new_location_type_pantry) {
            return LocationType.PANTRY;
        } else if (radioButtonId == R.id.new_location_type_emergency_kit) {
            return LocationType.EMERGENCY_KIT;
        } else if (radioButtonId == R.id.new_location_type_first_aid) {
            return LocationType.FIRST_AID;
        } else {
            return LocationType.OTHER;
        }
    }

    // ----- Small helpers -----

    /** Called with the new text after the household changes a text field. */
    private interface OnText {
        void onText(@NonNull String text);
    }

    @NonNull
    private static TextWatcher afterTextChanged(@NonNull OnText listener) {
        return new TextWatcher() {
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
                listener.onText(text.toString());
            }
        };
    }

    /** The field's text, with outer spaces removed when {@code trim} is true. */
    @NonNull
    private static String textOf(@NonNull EditText field, boolean trim) {
        String text = field.getText() == null ? "" : field.getText().toString();
        return trim ? text.trim() : text;
    }
}

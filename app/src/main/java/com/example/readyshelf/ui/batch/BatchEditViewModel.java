package com.example.readyshelf.ui.batch;

import android.app.Application;
import android.content.Context;

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
import com.example.readyshelf.data.Batch;
import com.example.readyshelf.data.DataSet;
import com.example.readyshelf.data.Product;
import com.example.readyshelf.data.PurchaseListStore;
import com.example.readyshelf.data.ReadyShelfRepository.SaveListener;
import com.example.readyshelf.data.ReadyShelfRepository.SaveResult;
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
import com.example.readyshelf.ui.common.DisplayNames;
import com.example.readyshelf.ui.photo.PhotoEntryResult;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Keeps the Add/Edit batch form's choices, builds the review, and saves the batch.
 * It also puts in what the household chose on the Label photos screen, and records a bought item
 * from the purchase list ("Record bought item": a new batch of the item's product, with its
 * printed date).
 *
 * <p>The choices are kept in a SavedStateHandle. That way they survive both screen rotation and
 * Android closing the app while the Label photos screen is open. Most text fields keep their own
 * text, so they are not stored here. The product search text and the printed date text are stored
 * here, because a loaded batch or a label photo can fill them in.
 */
public class BatchEditViewModel extends AndroidViewModel {

    // Keys for the values kept in the SavedStateHandle.
    private static final String KEY_FORM_FILLED = "formFilled";
    private static final String KEY_NEW_PRODUCT = "newProduct";
    private static final String KEY_PRODUCT_ID = "productId";
    private static final String KEY_PRODUCT_SEARCH = "productSearch";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_CATEGORY_BY_HAND = "categoryByHand";
    private static final String KEY_CATEGORY_CUE = "categoryCue";
    private static final String KEY_LOCATION_ID = "locationId";
    private static final String KEY_DATE_LABEL = "dateLabel";
    private static final String KEY_DATE_KIND = "dateKind";
    private static final String KEY_DATE = "date";
    private static final String KEY_PRINTED_DATE = "printedDate";
    private static final String KEY_DATE_CONFIRMED = "dateConfirmed";
    private static final String KEY_DATE_FROM_PHOTO = "dateFromPhoto";
    private static final String KEY_DATE_MONTH_ONLY = "dateMonthOnly";
    private static final String KEY_DATE_MONTH_END = "dateMonthEnd";
    private static final String KEY_DATE_AS_PRINTED = "dateAsPrinted";
    private static final String KEY_DATE_PHOTO_TEXT = "datePhotoText";
    private static final String KEY_DATE_LABEL_OPEN = "dateLabelOpen";
    private static final String KEY_DATE_KIND_OPEN = "dateKindOpen";
    private static final String KEY_PHOTO_RESULT = "photoResult";
    private static final String KEY_PHOTO_CATEGORY_APPLIED = "photoCategoryApplied";
    private static final String KEY_PURCHASE_FILLED = "purchaseFilled";
    private static final String KEY_PURCHASE_QUANTITY_TAKEN = "purchaseQuantityTaken";
    private static final String KEY_CARRIED_OVER = "carriedOver";
    private static final String KEY_REVIEW_INPUT = "reviewInput";
    private static final String KEY_TOUCHED = "touched";
    private static final String KEY_TEXT_BASELINE = "textBaseline";

    /** What the household typed into the plain text fields, kept while the review is shown. */
    public static final class TypedInput implements Serializable {
        private static final long serialVersionUID = 1L;

        @NonNull final String newName;
        @NonNull final String newUnit;
        final int quantity;

        /** The name and unit are only used when "New product" is chosen. */
        public TypedInput(@NonNull String newName, @NonNull String newUnit, int quantity) {
            this.newName = newName;
            this.newUnit = newUnit;
            this.quantity = quantity;
        }
    }

    /** What the label photos part, the notes, and the review show. */
    public static final class State {
        @Nullable public PhotoEntryResult photoResult; // the last result of the Label photos screen
        public boolean photoCategoryApplied;     // ...and it changed the category
        public boolean dateFromPhoto;            // the date shown came from a photo
        public boolean dateMonthOnly;            // ...and only its month and year were printed
        public boolean dateAsPrinted;            // ...or it can be read two ways, so it is kept as printed
        @Nullable public String monthEndFrom;    // the date is the last day of the month in this printed text
        @Nullable public String categoryCue;     // the category came from a photo because of these words

        public boolean reviewing;                // the review is shown instead of the form
        @NonNull public String reviewText = "";
        @Nullable public String kitWarning;      // kit amounts and "Replacement may be needed"
        @Nullable public String purchaseNote;    // "From your purchase list: ...", when recording one
        public boolean saving;
        @StringRes public int saveError;         // 0 when there is no error
        public boolean done;                     // saved, so the screen closes
        public boolean addAnother;               // ...and an empty form opens for the next batch
        public boolean nothingChanged;           // ...or an edit changed nothing, so nothing was saved
    }

    /** The product and batch that "Confirm and save" would save. */
    private static final class Draft {
        @Nullable Product newProduct;            // set when a new product will be added
        @NonNull String productText = "";        // the product on one line
        @NonNull String unit = "";
        final Batch batch = new Batch();
    }

    private final AppContainer app;
    // The household's records or the sample data, chosen once when this screen is made
    // (see AppContainer). Every switch makes all screens again.
    private final DataSet data;
    private final SavedStateHandle saved;
    private final LiveData<List<StorageLocation>> locations;
    private final LiveData<List<Product>> products;
    private final LiveData<List<BatchInfo>> activeBatches;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final MutableLiveData<Integer> locationMessage = new MutableLiveData<>();
    private final Executor mainThread;

    private boolean started;
    private long batchId;                  // 0 means "add a new batch"
    @Nullable private LiveData<BatchInfo> batchToEdit;
    @Nullable private BatchInfo original;  // the batch as it was before editing
    @Nullable private String newLocationName; // select this location when it appears
    // Recording a bought item from the purchase list. The Intent keeps these values.
    private long purchaseItemId;           // 0 when not recording a purchase
    private long purchaseKitLocationId;
    private int purchaseQuantity;

    // Saving
    private boolean saving;
    @StringRes private int saveError;
    private boolean done;
    private boolean addAnother;            // "Save and add another" was tapped
    private boolean nothingChanged;        // an edit with no change was confirmed: nothing saved

    public BatchEditViewModel(@NonNull Application application, @NonNull SavedStateHandle savedState) {
        super(application);
        app = ReadyShelfApp.containerOf(application);
        data = app.dataSets.active();
        saved = savedState;
        locations = data.repository.observeLocations();
        products = data.repository.observeProducts();
        activeBatches = data.repository.observeBatches();
        mainThread = ContextCompat.getMainExecutor(application);
        // The review uses products, locations, and kit amounts, so it is rebuilt when they change.
        state.addSource(products, value -> {
            fillPurchaseProductText();
            publish();
        });
        state.addSource(locations, value -> publish());
        state.addSource(activeBatches, value -> publish());
        publish();
    }

    /** The Activity calls this in onCreate. 0 means "add a new batch". Later calls do nothing. */
    public void start(long batchIdToEdit) {
        if (started) {
            return;
        }
        started = true;
        batchId = batchIdToEdit;
        if (batchId != 0) {
            batchToEdit = data.repository.observeBatchInfo(batchId);
        }
        publish();
    }

    /**
     * The Activity calls this in onCreate, instead of {@link #start}, to record a bought item
     * from the purchase list as a new batch. The product is the item's product and can't be
     * changed on the form. Later calls do nothing.
     */
    public void startPurchase(long itemId, long productId, long kitLocationId, int quantity) {
        if (started) {
            return;
        }
        purchaseItemId = itemId;
        purchaseKitLocationId = kitLocationId;
        purchaseQuantity = quantity;
        if (!flag(KEY_PURCHASE_FILLED)) {
            saved.set(KEY_PURCHASE_FILLED, true);
            saved.set(KEY_NEW_PRODUCT, false);
            saved.set(KEY_PRODUCT_ID, productId);
        }
        start(0);
        fillPurchaseProductText();
        publish();
    }

    /**
     * The Activity calls this in onCreate, instead of {@link #start}, after "Save and add
     * another": a new batch that starts with the category and location of the batch just saved.
     * The household chose that category, so a label photo's guess never replaces it (except
     * Medicine). Later calls do nothing.
     */
    public void startAnother(@NonNull ItemCategory category, long locationId) {
        if (started) {
            return;
        }
        if (!flag(KEY_CARRIED_OVER)) {
            saved.set(KEY_CARRIED_OVER, true);
            saved.set(KEY_CATEGORY, category);
            saved.set(KEY_CATEGORY_BY_HAND, true);
            saved.set(KEY_LOCATION_ID, locationId);
        }
        start(0);
    }

    public boolean isEditing() {
        return batchId != 0;
    }

    /** True when the form records a bought item from the purchase list. */
    public boolean isPurchase() {
        return purchaseItemId != 0;
    }

    /**
     * The amount from the purchase list, for the quantity field. It is given only once, so a
     * rotated screen keeps what the household typed. Null afterwards and when not recording one.
     */
    @Nullable
    public Integer takePurchaseQuantity() {
        if (!isPurchase() || flag(KEY_PURCHASE_QUANTITY_TAKEN)) {
            return null;
        }
        saved.set(KEY_PURCHASE_QUANTITY_TAKEN, true);
        return purchaseQuantity;
    }

    /** Shows the purchase item's product in the product field once the product list has loaded. */
    private void fillPurchaseProductText() {
        Product product = isPurchase() ? selectedProduct() : null;
        if (product != null && !choiceText(product).equals(getProductSearchText())) {
            saved.set(KEY_PRODUCT_SEARCH, choiceText(product));
        }
    }

    /** The batch being edited (its value is null if it no longer exists). Null when adding. */
    @Nullable
    public LiveData<BatchInfo> getBatchToEdit() {
        return batchToEdit;
    }

    @NonNull
    public LiveData<List<StorageLocation>> getLocations() {
        return locations;
    }

    @NonNull
    public LiveData<List<Product>> getProducts() {
        return products;
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    /**
     * Copies the loaded batch into the form's choices. This happens only once, so a rotated or
     * reopened screen keeps the household's changes. Returns true when the Activity should fill
     * its text fields now.
     */
    public boolean takeLoadedBatch(@NonNull BatchInfo batch) {
        if (original == null) {
            original = batch;
        }
        if (flag(KEY_FORM_FILLED)) {
            publish();
            return false;
        }
        saved.set(KEY_FORM_FILLED, true);
        saved.set(KEY_NEW_PRODUCT, false);
        saved.set(KEY_PRODUCT_ID, batch.getProductId());
        saved.set(KEY_PRODUCT_SEARCH, BatchText.productChoice(getApplication(), batch));
        saved.set(KEY_CATEGORY, batch.getCategory());
        saved.set(KEY_LOCATION_ID, batch.getLocationId());
        saved.set(KEY_DATE_LABEL, batch.getDateLabel());
        putDateKind(DateEntry.kindOf(batch));
        saved.set(KEY_DATE, batch.getDate());
        saved.set(KEY_PRINTED_DATE, batch.getPrintedDateText() == null ? "" : batch.getPrintedDateText());
        saved.set(KEY_DATE_CONFIRMED, batch.isDateConfirmed());
        publish();
        return true;
    }

    // ----- Leaving without saving -----

    /**
     * True after the household changed something on the form by hand: a choice, the date or its
     * tick, the product, or what the label photos gave. Then Back asks "Leave without saving?"
     * first (BatchEditActivity). What the screen fills in by itself doesn't count.
     */
    public boolean isTouched() {
        return flag(KEY_TOUCHED);
    }

    /** The household changed something on the form by hand. */
    public void markTouched() {
        saved.set(KEY_TOUCHED, true);
    }

    /**
     * The typed fields as the screen filled them (empty, or the quantity it started with), or
     * null before that. A field that differs from it was typed by the household.
     */
    @Nullable
    public ArrayList<String> getTextBaseline() {
        return saved.get(KEY_TEXT_BASELINE);
    }

    public void setTextBaseline(@NonNull ArrayList<String> texts) {
        saved.set(KEY_TEXT_BASELINE, texts);
    }

    /**
     * True when leaving the form now would lose something the household entered: a change made
     * by hand ({@link #isTouched}), or a typed field that differs from how the screen filled it.
     *
     * @param typed the name, unit, and quantity fields, in that order
     */
    public boolean hasUnsavedInput(@NonNull List<String> typed) {
        if (isTouched()) {
            return true;
        }
        List<String> filled = getTextBaseline();
        return filled != null && !filled.equals(typed);
    }

    /** True when editing a sample batch. */
    public boolean isSample() {
        return original != null && original.isSample();
    }

    // ----- Product -----

    /**
     * True when the form shows "New product". Until the household chooses, a new batch starts
     * with "New product" only when there are no products to choose from.
     */
    public boolean isNewProduct() {
        Boolean chosen = saved.get(KEY_NEW_PRODUCT);
        if (chosen != null) {
            return chosen;
        }
        return !isEditing() && products.getValue() != null && choosableProducts().isEmpty();
    }

    public void setNewProduct(boolean newProduct) {
        Boolean chosen = saved.get(KEY_NEW_PRODUCT);
        if (chosen != null && chosen == newProduct) {
            return;
        }
        saved.set(KEY_NEW_PRODUCT, newProduct);
        publish();
    }

    /** True once the product list has loaded. */
    public boolean areProductsLoaded() {
        return products.getValue() != null;
    }

    /**
     * The products the form offers: the household's own products, plus the chosen product
     * (which can be a sample product when a sample batch is edited).
     */
    @NonNull
    public List<Product> choosableProducts() {
        List<Product> all = products.getValue();
        if (all == null) {
            return Collections.emptyList();
        }
        long chosenId = getProductId();
        List<Product> result = new ArrayList<>();
        for (Product product : all) {
            if (!product.sample || product.id == chosenId) {
                result.add(product);
            }
        }
        return result;
    }

    /** The chosen product's id, or 0 when no product is chosen. */
    public long getProductId() {
        Long id = saved.get(KEY_PRODUCT_ID);
        return id != null ? id : 0;
    }

    /** The chosen product, or null when none is chosen (or the list hasn't loaded yet). */
    @Nullable
    public Product selectedProduct() {
        long chosenId = getProductId();
        List<Product> all = products.getValue();
        if (chosenId == 0 || all == null) {
            return null;
        }
        for (Product product : all) {
            if (product.id == chosenId) {
                return product;
            }
        }
        return null;
    }

    /** The household tapped a product in the list. */
    public void selectProduct(long productId) {
        saved.set(KEY_PRODUCT_ID, productId);
        Product product = selectedProduct();
        saved.set(KEY_PRODUCT_SEARCH, product == null ? "" : choiceText(product));
        publish();
    }

    /** The text in the product search field. */
    @NonNull
    public String getProductSearchText() {
        String text = saved.get(KEY_PRODUCT_SEARCH);
        return text != null ? text : "";
    }

    /** The household typed in the product search field. Typing away from a chosen product unchooses it. */
    public void setProductSearchText(@NonNull String text) {
        if (text.equals(getProductSearchText())) {
            return;
        }
        saved.set(KEY_PRODUCT_SEARCH, text);
        Product product = selectedProduct();
        if (product != null && !text.equals(choiceText(product))) {
            saved.set(KEY_PRODUCT_ID, 0L);
        }
        publish();
    }

    /** One product on one line, with its variant, package size, and unit. */
    @NonNull
    public String choiceText(@NonNull Product product) {
        return BatchText.productChoice(getApplication(), product.name, product.variant,
                product.packageSize, product.unit);
    }

    /** The chosen product's unit, or "" when no product is chosen. */
    @NonNull
    public String selectedUnit() {
        Product product = selectedProduct();
        return product == null ? "" : product.unit;
    }

    /**
     * The household's product that a new product with this name and unit would be (capital
     * letters don't matter), or null. Used to stop the same product from being added twice.
     * A new product has no variant or package size, so only a product without them matches.
     */
    @Nullable
    public Product findSameProduct(@NonNull String name, @NonNull String unit) {
        List<Product> all = products.getValue();
        if (all == null) {
            return null;
        }
        for (Product product : all) {
            if (!product.sample && product.name.equalsIgnoreCase(name)
                    && product.variant.isEmpty() && product.packageSize.isEmpty()
                    && product.unit.equalsIgnoreCase(unit)) {
                return product;
            }
        }
        return null;
    }

    /**
     * The name of one of the household's products that matches a line from a label photo
     * (capital letters don't matter), spelled as the product spells it. Null when none matches.
     */
    @Nullable
    public String existingProductName(@NonNull String line) {
        for (Product product : choosableProducts()) {
            if (product.name.equalsIgnoreCase(line)) {
                return product.name;
            }
        }
        return null;
    }

    // ----- Category and location. Each setter ignores a value that didn't change. -----

    @NonNull
    public ItemCategory getCategory() {
        ItemCategory category = saved.get(KEY_CATEGORY);
        return category != null ? category : ItemCategory.FOOD;
    }

    /** The household chose a category. A guess from a photo never replaces it, except Medicine. */
    public void setCategory(@NonNull ItemCategory newCategory) {
        if (newCategory == getCategory()) {
            return;
        }
        DateLabel labelBefore = getDateLabel();
        saved.set(KEY_CATEGORY, newCategory);
        saved.set(KEY_CATEGORY_BY_HAND, true);
        saved.set(KEY_CATEGORY_CUE, null);
        uncheckIfLabelChanged(labelBefore);
        publish();
    }

    /**
     * True for a new document or subscription: one item, so the form asks for no quantity and
     * no unit (CategoryDefaults). Editing and recording a bought item always ask for them.
     */
    public boolean isOneItem() {
        return !isEditing() && !isPurchase() && CategoryDefaults.isOneItem(getCategory());
    }

    /** The unit a new product gets when {@link #isOneItem()} ("document"), otherwise "". */
    @NonNull
    public String oneItemUnit() {
        return isOneItem() ? getApplication().getString(CategoryDefaults.oneItemUnit(getCategory())) : "";
    }

    public long getLocationId() {
        Long id = saved.get(KEY_LOCATION_ID);
        return id != null ? id : 0;
    }

    public void setLocationId(long newLocationId) {
        saved.set(KEY_LOCATION_ID, newLocationId);
    }

    /** True when a location already has this name (capital letters don't matter). */
    public boolean isLocationNameTaken(@NonNull String name) {
        List<StorageLocation> current = locations.getValue();
        if (current == null) {
            return false;
        }
        for (StorageLocation location : current) {
            if (location.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** Adds a location. The form selects it when it appears in the list. */
    public void addLocation(@NonNull String name, @NonNull LocationType type) {
        newLocationName = name;
        data.repository.addLocation(name, type, result -> mainThread.execute(() -> {
            if (result != SaveResult.SAVED) {
                newLocationName = null;
                locationMessage.setValue(R.string.new_location_failed);
            }
        }));
    }

    /** A message about adding a location, for the screen to show once, or null. */
    @NonNull
    public LiveData<Integer> getLocationMessage() {
        return locationMessage;
    }

    /** The screen showed the message, so it isn't shown again after the screen rotates. */
    public void locationMessageShown() {
        locationMessage.setValue(null);
    }

    /**
     * Decides which location the form shows as chosen: a location that was just added,
     * otherwise the current choice, otherwise the first one. Returns its position in
     * {@code list}, or -1 when the list is empty.
     */
    public int chooseLocationPosition(@NonNull List<StorageLocation> list) {
        if (newLocationName != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).name.equalsIgnoreCase(newLocationName)) {
                    newLocationName = null;
                    setLocationId(list.get(i).id);
                    return i;
                }
            }
        }
        long chosenId = getLocationId();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == chosenId) {
                return i;
            }
        }
        if (list.isEmpty()) {
            return -1;
        }
        setLocationId(list.get(0).id);
        return 0;
    }

    // ----- Date. Changing the date or its label type removes the confirmation. -----

    /**
     * The label type. Until the household chooses one (or a label photo or the batch being
     * edited gives one), it follows the category, for example "Expires" for Medicine.
     */
    @NonNull
    public DateLabel getDateLabel() {
        DateLabel label = saved.get(KEY_DATE_LABEL);
        return label != null ? label : CategoryDefaults.startingLabel(getCategory());
    }

    /**
     * After the category changed: when the label type followed it to another one, the
     * confirmation is removed, because it was given for the date with the old label type.
     */
    private void uncheckIfLabelChanged(@NonNull DateLabel labelBefore) {
        if (getDateLabel() != labelBefore) {
            saved.set(KEY_DATE_CONFIRMED, false);
        }
    }

    public void setDateLabel(@NonNull DateLabel newLabel) {
        if (newLabel == getDateLabel()) {
            return;
        }
        saved.set(KEY_DATE_LABEL, newLabel);
        saved.set(KEY_DATE_CONFIRMED, false);
        publish();
    }

    /** What is printed: a complete date, part of a date, or no date. */
    @NonNull
    public DateEntry.Kind getDateKind() {
        DateEntry.Kind kind = saved.get(KEY_DATE_KIND);
        return kind != null ? kind : DateEntry.Kind.COMPLETE;
    }

    public void setDateKind(@NonNull DateEntry.Kind newKind) {
        if (newKind == getDateKind()) {
            return;
        }
        putDateKind(newKind);
        dateChangedByHand();
    }

    /**
     * Keeps what is printed. Anything but a full date also opens "What is printed?" for the rest
     * of this form, so the chosen answer stays in view, also after "Use the last day of the month".
     */
    private void putDateKind(@NonNull DateEntry.Kind kind) {
        saved.set(KEY_DATE_KIND, kind);
        if (kind != DateEntry.Kind.COMPLETE) {
            saved.set(KEY_DATE_KIND_OPEN, true);
        }
    }

    // ----- Folded parts of the date. Opening one is not something the household entered. -----

    /** True once "Change" was tapped: the label type chips show instead of the one-line label. */
    public boolean isDateLabelOpen() {
        return flag(KEY_DATE_LABEL_OPEN);
    }

    /** The household tapped "Change" beside the label type. The chips stay open on this form. */
    public void openDateLabel() {
        if (isDateLabelOpen()) {
            return;
        }
        saved.set(KEY_DATE_LABEL_OPEN, true);
        publish();
    }

    /**
     * True when "What is printed?" shows: after "No full date on the package?" was tapped, or
     * "Review and save" found no date, and whenever the date isn't a full date.
     */
    public boolean isDateKindOpen() {
        return flag(KEY_DATE_KIND_OPEN) || getDateKind() != DateEntry.Kind.COMPLETE;
    }

    /** Opens "What is printed?" for the rest of this form. */
    public void openDateKind() {
        if (flag(KEY_DATE_KIND_OPEN)) {
            return;
        }
        saved.set(KEY_DATE_KIND_OPEN, true);
        publish();
    }

    /** The complete date chosen with the date picker, or null when none is chosen yet. */
    @Nullable
    public LocalDate getDate() {
        return saved.get(KEY_DATE);
    }

    /** The household chose a date with the date picker. Past dates are allowed. */
    public void setDate(@NonNull LocalDate newDate) {
        if (newDate.equals(getDate())) {
            return;
        }
        saved.set(KEY_DATE, newDate);
        dateChangedByHand();
    }

    /** What the label shows, for an incomplete date. Kept exactly as typed. */
    @NonNull
    public String getPrintedDateText() {
        String text = saved.get(KEY_PRINTED_DATE);
        return text != null ? text : "";
    }

    public void setPrintedDateText(@NonNull String text) {
        if (text.equals(getPrintedDateText())) {
            return;
        }
        saved.set(KEY_PRINTED_DATE, text);
        dateChangedByHand();
    }

    /** True when the household ticked "I checked that this date and its label type match". */
    public boolean isDateConfirmed() {
        return flag(KEY_DATE_CONFIRMED);
    }

    public void setDateConfirmed(boolean confirmed) {
        if (confirmed == isDateConfirmed()) {
            return;
        }
        saved.set(KEY_DATE_CONFIRMED, confirmed);
        publish();
    }

    /** Why the date needs review, or null when it doesn't. Uses only what would be saved. */
    @Nullable
    public String getDateReviewText() {
        DateEntry.Kind kind = getDateKind();
        LocalDate date = kind == DateEntry.Kind.COMPLETE ? getDate() : null;
        String printed = kind == DateEntry.Kind.INCOMPLETE ? getPrintedDateText() : null;
        return BatchText.dateReview(getApplication(), date, printed, date != null && isDateConfirmed());
    }

    /**
     * The date the "Use the last day of the month" button offers, or null when the button is
     * hidden: the date isn't incomplete, or the printed text isn't exactly one month and year.
     */
    @Nullable
    public LocalDate getMonthEndChoice() {
        if (getDateKind() != DateEntry.Kind.INCOMPLETE) {
            return null;
        }
        return DateEntry.lastDayOfPrintedMonth(getPrintedDateText());
    }

    /**
     * The household tapped "Use the last day of the month". The form switches to that complete
     * date, unconfirmed. The printed text is kept, so choosing "Only part of a date" again
     * brings it back.
     */
    public void useMonthEnd() {
        LocalDate monthEnd = getMonthEndChoice();
        if (monthEnd == null) {
            return;
        }
        putDateKind(DateEntry.Kind.COMPLETE);
        saved.set(KEY_DATE, monthEnd);
        saved.set(KEY_DATE_CONFIRMED, false);
        saved.set(KEY_DATE_MONTH_ONLY, false); // it is no longer kept as an incomplete date
        saved.set(KEY_DATE_AS_PRINTED, false);
        saved.set(KEY_DATE_MONTH_END, true);
        publish();
    }

    /**
     * The printed text whose last day of the month is the complete date, while the household
     * hasn't changed the date since tapping the button. Null otherwise.
     */
    @Nullable
    private String monthEndSource() {
        if (!flag(KEY_DATE_MONTH_END) || getDateKind() != DateEntry.Kind.COMPLETE) {
            return null;
        }
        return getPrintedDateText().trim();
    }

    private void dateChangedByHand() {
        saved.set(KEY_DATE_CONFIRMED, false);
        saved.set(KEY_DATE_FROM_PHOTO, false);
        saved.set(KEY_DATE_MONTH_ONLY, false);
        saved.set(KEY_DATE_AS_PRINTED, false);
        saved.set(KEY_DATE_PHOTO_TEXT, null);
        saved.set(KEY_DATE_MONTH_END, false);
        publish();
    }

    /**
     * The label text that the date was read from, while the form still shows the date that the
     * household chose on the Label photos screen. Null otherwise.
     */
    @Nullable
    private String datePhotoText() {
        return flag(KEY_DATE_FROM_PHOTO) ? saved.get(KEY_DATE_PHOTO_TEXT) : null;
    }

    // ----- Review and saving -----

    /** Shows the review. The Activity has already checked every field. */
    public void startReview(@NonNull TypedInput input) {
        saved.set(KEY_REVIEW_INPUT, input);
        saveError = 0;
        publish();
    }

    /** Goes back from the review to the form. Nothing was saved. */
    public void cancelReview() {
        if (saving) {
            return;
        }
        saved.set(KEY_REVIEW_INPUT, null);
        saveError = 0;
        publish();
    }

    /** True when the review offers "Save and add another": only when adding a new batch. */
    public boolean canAddAnother() {
        return !isEditing() && !isPurchase();
    }

    /**
     * Saves what the review shows. The screen closes when the save is done; with
     * {@code thenAddAnother} (see {@link #canAddAnother()}), an empty form opens for the next batch.
     */
    public void confirmSave(boolean thenAddAnother) {
        TypedInput input = saved.get(KEY_REVIEW_INPUT);
        if (input == null || saving || done || (isEditing() && original == null)) {
            return;
        }
        addAnother = thenAddAnother && canAddAnother();
        Draft draft = buildDraft(input);
        if (draft == null) {
            saveError = R.string.error_product_required;
            publish();
            return;
        }
        SaveListener listener = result -> mainThread.execute(() -> saveFinished(result));
        if (original == null) {
            String details = getApplication().getString(R.string.history_added,
                    amount(draft.batch.quantity, draft.unit), locationName(draft.batch.locationId));
            if (isPurchase()) {
                details += " " + getApplication().getString(R.string.history_bought_from_list);
            }
            String photoText = datePhotoText();
            if (photoText != null) {
                details += " " + getApplication().getString(R.string.history_date_from_photo, photoText);
            }
            String monthEndFrom = monthEndSource();
            if (monthEndFrom != null) {
                details += " " + getApplication().getString(R.string.history_date_month_end, monthEndFrom);
            }
            saving = true;
            if (isPurchase()) {
                // The new batch and the removal from the purchase list are saved together.
                data.repository.recordPurchase(purchaseItemId, draft.batch, details,
                        result -> mainThread.execute(() -> purchaseFinished(result)));
            } else {
                data.repository.addBatch(draft.newProduct, draft.batch, details, listener);
            }
        } else {
            List<String> changes = changeSentences(original, draft);
            if (changes.isEmpty()) {
                // Nothing to save. The screen closes and says so ("No changes to save."), not
                // "Changes saved.".
                done = true;
                nothingChanged = true;
            } else {
                saving = true;
                data.repository.updateBatch(draft.newProduct, draft.batch, original.getQuantity(),
                        String.join(" ", changes), listener);
            }
        }
        saveError = 0;
        publish();
    }

    private void saveFinished(@NonNull SaveResult result) {
        saving = false;
        if (result == SaveResult.SAVED) {
            done = true;
        } else if (result == SaveResult.BATCH_CHANGED) {
            saveError = R.string.error_batch_changed;
            // Compare later changes with the batch as it is now.
            BatchInfo latest = batchToEdit != null ? batchToEdit.getValue() : null;
            if (latest != null) {
                original = latest;
            }
        } else {
            saveError = R.string.error_not_saved;
        }
        publish();
    }

    private void purchaseFinished(@NonNull PurchaseListStore.Result result) {
        saving = false;
        if (result == PurchaseListStore.Result.SAVED) {
            done = true;
        } else if (result == PurchaseListStore.Result.ITEM_CHANGED) {
            // The item is gone, for example because this purchase was already recorded.
            saveError = R.string.error_purchase_item_gone;
        } else {
            saveError = R.string.error_not_saved;
        }
        publish();
    }

    /** The product and batch to save, or null when the chosen product is gone. */
    @Nullable
    private Draft buildDraft(@NonNull TypedInput input) {
        Draft draft = new Draft();
        if (isNewProduct()) {
            // A new product keeps Product's empty variant and package size and has no food
            // class: the form doesn't ask for them (since 2026-10-22).
            Product product = new Product();
            product.name = input.newName;
            product.unit = input.newUnit;
            draft.newProduct = product;
            draft.productText = choiceText(product);
            draft.unit = product.unit;
        } else {
            Product product = selectedProduct();
            if (product == null) {
                return null;
            }
            draft.batch.productId = product.id;
            draft.productText = choiceText(product);
            draft.unit = product.unit;
        }

        // The form has no food values since 2026-10-22: a new batch has none, and an edit keeps
        // the stored ones (ReadyShelfRepository.updateBatch doesn't write them).
        Batch batch = draft.batch;
        batch.category = getCategory();
        batch.quantity = input.quantity;
        DateEntry.Kind kind = getDateKind();
        batch.date = kind == DateEntry.Kind.COMPLETE ? getDate() : null;
        batch.printedDateText = kind == DateEntry.Kind.INCOMPLETE ? getPrintedDateText().trim() : null;
        batch.dateLabel = getDateLabel();
        batch.dateConfirmed = batch.date != null && isDateConfirmed();
        if (original != null) {
            batch.id = original.getId();
            batch.locationId = original.getLocationId();
            batch.sample = original.isSample();
        } else {
            batch.locationId = getLocationId();
        }
        return draft;
    }

    /** Fills in the review text and the kit warning. */
    private void buildReview(@NonNull TypedInput input, @NonNull State next) {
        if (isEditing() && original == null) {
            return; // the batch is still loading
        }
        Draft draft = buildDraft(input);
        if (draft == null) {
            return;
        }
        Context context = getApplication();
        List<String> lines;
        String intro;
        if (original == null) {
            intro = context.getString(isPurchase() ? R.string.form_review_intro_purchase
                    : R.string.form_review_intro_add);
            lines = addedLines(draft);
            if (isPurchase()) {
                lines.add(context.getString(R.string.review_purchase_item,
                        amount(purchaseQuantity, draft.unit), draft.productText,
                        locationName(purchaseKitLocationId)));
            }
        } else {
            lines = changeSentences(original, draft);
            intro = context.getString(lines.isEmpty() ? R.string.form_review_no_changes
                    : R.string.form_review_intro_edit);
            List<BatchInfo> active = activeBatches.getValue();
            StockChange change = StockChange.countCorrection(original,
                    draft.newProduct != null ? 0 : draft.batch.productId, draft.batch.quantity,
                    active != null ? active : Collections.emptyList());
            StockChange.Amounts kit = change.getKit();
            if (kit != null) {
                next.kitWarning = context.getString(R.string.kit_amounts, original.getName(),
                        original.getLocationName(), amount(kit.getBefore(), original.getUnit()),
                        amount(kit.getAfter(), original.getUnit()))
                        + " " + context.getString(R.string.kit_warning);
            }
        }
        StringBuilder text = new StringBuilder(intro);
        for (String line : lines) {
            text.append("\n• ").append(line);
        }
        next.reviewText = text.toString();
    }

    /** The review lines for a new batch. */
    @NonNull
    private List<String> addedLines(@NonNull Draft draft) {
        Context context = getApplication();
        Batch batch = draft.batch;
        List<String> lines = new ArrayList<>();
        lines.add(context.getString(draft.newProduct != null ? R.string.review_product_new
                : R.string.review_product, draft.productText));
        lines.add(context.getString(R.string.review_category,
                context.getString(DisplayNames.category(batch.category))));
        lines.add(context.getString(R.string.review_location, locationName(batch.locationId)));
        lines.add(context.getString(R.string.review_quantity, amount(batch.quantity, draft.unit)));
        lines.add(context.getString(R.string.review_date, BatchText.dateEntry(context, batch.date,
                batch.printedDateText, batch.dateLabel)));
        String photoText = datePhotoText();
        if (photoText != null) {
            lines.add(context.getString(R.string.history_date_from_photo, photoText));
        }
        String monthEndFrom = monthEndSource();
        if (monthEndFrom != null) {
            lines.add(context.getString(R.string.history_date_month_end, monthEndFrom));
        }
        String dateReview = BatchText.dateReview(context, batch.date, batch.printedDateText,
                batch.dateConfirmed);
        if (dateReview != null) {
            lines.add(dateReview);
        }
        return lines;
    }

    /**
     * One sentence for each change, for the review and the history, for example
     * "Count corrected from 6 bars to 5 bars." Empty when nothing changed.
     */
    @NonNull
    private List<String> changeSentences(@NonNull BatchInfo before, @NonNull Draft after) {
        Context context = getApplication();
        Batch batch = after.batch;
        List<String> sentences = new ArrayList<>();
        if (after.newProduct != null || batch.productId != before.getProductId()) {
            sentences.add(context.getString(R.string.history_product_changed,
                    BatchText.productChoice(context, before), after.productText));
        }
        if (before.getCategory() != batch.category) {
            sentences.add(context.getString(R.string.history_category_changed,
                    context.getString(DisplayNames.category(before.getCategory())),
                    context.getString(DisplayNames.category(batch.category))));
        }
        if (before.getQuantity() != batch.quantity) {
            sentences.add(context.getString(R.string.history_count_corrected,
                    amount(before.getQuantity(), before.getUnit()), amount(batch.quantity, after.unit)));
        }
        String beforeDate = BatchText.dateEntry(context, before);
        String afterDate = BatchText.dateEntry(context, batch.date, batch.printedDateText, batch.dateLabel);
        if (!beforeDate.equals(afterDate)) {
            sentences.add(context.getString(R.string.history_date_changed, beforeDate, afterDate));
            String monthEndFrom = monthEndSource();
            if (monthEndFrom != null) {
                sentences.add(context.getString(R.string.history_date_month_end, monthEndFrom));
            }
        }
        if (before.isDateConfirmed() != batch.dateConfirmed) {
            sentences.add(context.getString(batch.dateConfirmed ? R.string.history_date_confirmed
                    : R.string.history_date_not_confirmed));
        }
        return sentences;
    }

    // ----- Label photos -----

    /**
     * The household came back from the Label photos screen. Only what they chose there goes into
     * the form: the date they tapped (unconfirmed), and the category clue under the usual rule.
     * The product is never chosen from a photo: the lines that could be the name are only offered
     * as buttons. Does nothing when editing or recording a bought item (the form hides the button).
     */
    public void applyPhotoResult(@NonNull PhotoEntryResult result) {
        if (isEditing() || isPurchase()) {
            return;
        }
        saved.set(KEY_PHOTO_RESULT, result);
        PhotoDateChoices.Choice date = result.getDate();
        if (date != null) {
            applyPhotoDate(date);
        }
        boolean categoryApplied = false;
        CategoryGuess guess = result.getCategoryGuess();
        if (guess != null && mayApplyCategory(guess.getCategory())) {
            DateLabel labelBefore = getDateLabel();
            saved.set(KEY_CATEGORY, guess.getCategory());
            saved.set(KEY_CATEGORY_CUE, guess.getCue());
            uncheckIfLabelChanged(labelBefore);
            categoryApplied = true;
        }
        saved.set(KEY_PHOTO_CATEGORY_APPLIED, categoryApplied);
        publish();
    }

    /**
     * A Medicine guess always applies, so medicine stays out of swaps. Other guesses never
     * replace Medicine, and never replace a category the household chose by hand.
     */
    private boolean mayApplyCategory(@NonNull ItemCategory guessed) {
        if (guessed == ItemCategory.MEDICINE) {
            return true;
        }
        return getCategory() != ItemCategory.MEDICINE && !flag(KEY_CATEGORY_BY_HAND);
    }

    /**
     * Puts the date the household tapped into the form, unconfirmed. A date printed only as a
     * month and year, or one that can be read two ways, becomes an incomplete date with the
     * printed text: no day is made up, and no reading is picked.
     */
    private void applyPhotoDate(@NonNull PhotoDateChoices.Choice date) {
        LocalDate complete = date.getDate();
        if (complete != null) {
            putDateKind(DateEntry.Kind.COMPLETE);
            saved.set(KEY_DATE, complete);
        } else {
            putDateKind(DateEntry.Kind.INCOMPLETE);
            saved.set(KEY_PRINTED_DATE, date.getPrintedText() == null ? "" : date.getPrintedText());
        }
        if (date.getLabel() != null) {
            saved.set(KEY_DATE_LABEL, date.getLabel());
        }
        saved.set(KEY_DATE_CONFIRMED, false);
        saved.set(KEY_DATE_FROM_PHOTO, true);
        saved.set(KEY_DATE_MONTH_ONLY,
                date.getReading() == PhotoDateChoices.Choice.Reading.MONTH_AND_YEAR);
        saved.set(KEY_DATE_AS_PRINTED,
                date.getReading() == PhotoDateChoices.Choice.Reading.AS_PRINTED);
        // A month-end choice made earlier belongs to the old date, not to this one.
        saved.set(KEY_DATE_MONTH_END, false);
        saved.set(KEY_DATE_PHOTO_TEXT, date.getRawText());
    }

    private boolean flag(@NonNull String key) {
        Boolean value = saved.get(key);
        return value != null && value;
    }

    private void publish() {
        State next = new State();
        next.photoResult = saved.get(KEY_PHOTO_RESULT);
        next.photoCategoryApplied = flag(KEY_PHOTO_CATEGORY_APPLIED);
        next.dateFromPhoto = flag(KEY_DATE_FROM_PHOTO);
        next.dateMonthOnly = flag(KEY_DATE_MONTH_ONLY);
        next.dateAsPrinted = flag(KEY_DATE_AS_PRINTED);
        next.monthEndFrom = monthEndSource();
        next.categoryCue = saved.get(KEY_CATEGORY_CUE);

        TypedInput input = saved.get(KEY_REVIEW_INPUT);
        next.reviewing = input != null;
        if (input != null && started) {
            buildReview(input, next);
        }
        next.purchaseNote = purchaseNote();
        next.saving = saving;
        next.saveError = saveError;
        next.done = done;
        next.addAnother = done && addAnother;
        next.nothingChanged = done && nothingChanged;
        state.setValue(next);
    }

    private String amount(int quantity, @NonNull String unit) {
        return BatchText.amount(getApplication(), quantity, unit);
    }

    /** "From your purchase list: 3 cans of Canned beans ... for Earthquake kit. ..." or null. */
    @Nullable
    private String purchaseNote() {
        Product product = isPurchase() ? selectedProduct() : null;
        if (product == null) {
            return null;
        }
        return getApplication().getString(R.string.form_purchase_note,
                amount(purchaseQuantity, product.unit), choiceText(product),
                locationName(purchaseKitLocationId));
    }

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

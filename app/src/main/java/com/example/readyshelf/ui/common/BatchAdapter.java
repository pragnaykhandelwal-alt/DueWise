package com.example.readyshelf.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.google.android.material.color.MaterialColors;

/**
 * Shows batch rows as cards in a RecyclerView (Inventory and the Purchase/Renewal list use it).
 * Tapping anywhere on a card calls the click listener. A row with an action also shows a button,
 * when the screen gave a listener for it; since 2026-10-24 a row can have a second one under it.
 */
public final class BatchAdapter extends ListAdapter<BatchRow, BatchAdapter.RowHolder> {

    /** What happens when a row is tapped. */
    public interface OnBatchClick {
        void onBatchClick(@NonNull BatchInfo batch);
    }

    /** What happens when one of a row's buttons (a BatchRow.Action) is tapped. */
    public interface OnBatchAction {
        void onBatchAction(@NonNull BatchRow row);
    }

    // Lets the list redraw only the rows that changed.
    private static final DiffUtil.ItemCallback<BatchRow> SAME_ROW =
            new DiffUtil.ItemCallback<BatchRow>() {
                @Override
                public boolean areItemsTheSame(@NonNull BatchRow oldRow, @NonNull BatchRow newRow) {
                    return oldRow.getBatch().getId() == newRow.getBatch().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BatchRow oldRow, @NonNull BatchRow newRow) {
                    return oldRow.equals(newRow);
                }
            };

    private final OnBatchClick onClick;
    @Nullable private final OnBatchAction onAction;
    @Nullable private final OnBatchAction onSecondAction;

    public BatchAdapter(@NonNull OnBatchClick onClick) {
        this(onClick, null);
    }

    public BatchAdapter(@NonNull OnBatchClick onClick, @Nullable OnBatchAction onAction) {
        this(onClick, onAction, null);
    }

    /**
     * @param onAction       what a tap on a row's first button does
     * @param onSecondAction what a tap on a row's second button does
     */
    public BatchAdapter(@NonNull OnBatchClick onClick, @Nullable OnBatchAction onAction,
                        @Nullable OnBatchAction onSecondAction) {
        super(SAME_ROW);
        this.onClick = onClick;
        this.onAction = onAction;
        this.onSecondAction = onSecondAction;
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_batch, parent, false);
        return new RowHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        holder.bind(getItem(position), onClick, onAction, onSecondAction);
    }

    /** Remembers the views of one row, so they are looked up only once. */
    static final class RowHolder extends RecyclerView.ViewHolder {
        private final ImageView categoryIcon;
        private final TextView name;
        private final TextView productDetails;
        private final TextView sample;
        private final TextView archived;
        private final TextView amount;
        private final TextView date;
        private final TextView dateReview;
        private final TextView status;
        private final TextView note;
        private final Button action;
        private final Button secondAction;

        RowHolder(@NonNull View view) {
            super(view);
            categoryIcon = view.findViewById(R.id.batch_category_icon);
            name = view.findViewById(R.id.batch_name);
            productDetails = view.findViewById(R.id.batch_product_details);
            sample = view.findViewById(R.id.batch_sample);
            archived = view.findViewById(R.id.batch_archived);
            amount = view.findViewById(R.id.batch_amount);
            date = view.findViewById(R.id.batch_date);
            dateReview = view.findViewById(R.id.batch_date_review);
            status = view.findViewById(R.id.batch_status);
            note = view.findViewById(R.id.batch_note);
            action = view.findViewById(R.id.batch_action);
            secondAction = view.findViewById(R.id.batch_second_action);
            StatusLook.setStartIcon(archived, R.drawable.ic_archive, MaterialColors.getColor(
                    archived, com.google.android.material.R.attr.colorOnSurfaceVariant));
        }

        void bind(@NonNull BatchRow row, @NonNull OnBatchClick onClick,
                  @Nullable OnBatchAction onAction, @Nullable OnBatchAction onSecondAction) {
            Context context = itemView.getContext();
            BatchInfo batch = row.getBatch();
            categoryIcon.setImageResource(CategoryLook.icon(batch.getCategory()));
            categoryIcon.setContentDescription(
                    context.getString(DisplayNames.category(batch.getCategory())));
            name.setText(batch.getName());
            String details = BatchText.productDetails(batch.getVariant(), batch.getPackageSize());
            productDetails.setText(details);
            productDetails.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);
            sample.setVisibility(batch.isSample() ? View.VISIBLE : View.GONE);
            archived.setVisibility(batch.isArchived() ? View.VISIBLE : View.GONE);
            amount.setText(BatchText.amountInPlace(context, batch));
            date.setText(BatchText.dateEntry(context, batch));
            String review = BatchText.dateReview(context, batch);
            dateReview.setText(review);
            dateReview.setVisibility(review == null ? View.GONE : View.VISIBLE);
            if (row.getStatus() == null) {
                status.setVisibility(View.GONE);
            } else {
                StatusLook.showLabel(status, row.getStatus());
                status.setVisibility(View.VISIBLE);
            }
            note.setText(row.getNote());
            note.setVisibility(row.getNote() == null ? View.GONE : View.VISIBLE);
            bindAction(action, row, row.getAction(), onAction);
            bindAction(secondAction, row, row.getSecondAction(), onSecondAction);
            itemView.setOnClickListener(view -> onClick.onBatchClick(batch));
        }

        /** Shows one of the row's buttons, or hides it when the row or the screen has none. */
        private static void bindAction(@NonNull Button button, @NonNull BatchRow row,
                                       @Nullable BatchRow.Action rowAction,
                                       @Nullable OnBatchAction onTap) {
            if (rowAction == null || onTap == null) {
                button.setVisibility(View.GONE);
                button.setOnClickListener(null);
            } else {
                button.setText(rowAction.getText());
                button.setContentDescription(rowAction.getDescription());
                button.setEnabled(rowAction.isEnabled());
                button.setOnClickListener(view -> onTap.onBatchAction(row));
                button.setVisibility(View.VISIBLE);
            }
        }
    }
}

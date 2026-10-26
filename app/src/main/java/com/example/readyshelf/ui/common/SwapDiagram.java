package com.example.readyshelf.ui.common;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.readyshelf.R;
import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.LocationType;
import com.example.readyshelf.domain.RuleRunner;
import com.google.android.material.color.MaterialColors;

/**
 * The swap picture (view_swap_diagram.xml), shared by Home's swap card and the Review swap
 * screen: "Earthquake kit → Pantry" with what leaves the kit for review, then "Pantry →
 * Earthquake kit" with what takes its place. TalkBack reads the two full sentences instead
 * (SwapText), so nothing in the picture is only shown and never said.
 *
 * <p>Since 2026-10-23 it has the colors of the student's design B1 on both screens: each place's
 * icon is gray next to its dark name, and "for review" is in dark brass, a little bolder.
 */
public final class SwapDiagram {

    /** What one swap picture shows. Never changes. */
    public static final class Content {
        @NonNull final String kitPlace;
        @NonNull final LocationType kitType;
        @NonNull final String pantryPlace;
        @NonNull final LocationType pantryType;
        // Styled text: "for review" has its own color and weight.
        @NonNull final CharSequence outDetails;
        @NonNull final String inDetails;
        @NonNull final String spoken;

        Content(@NonNull String kitPlace, @NonNull LocationType kitType,
                @NonNull String pantryPlace, @NonNull LocationType pantryType,
                @NonNull CharSequence outDetails, @NonNull String inDetails,
                @NonNull String spoken) {
            this.kitPlace = kitPlace;
            this.kitType = kitType;
            this.pantryPlace = pantryPlace;
            this.pantryType = pantryType;
            this.outDetails = outDetails;
            this.inDetails = inDetails;
            this.spoken = spoken;
        }
    }

    private SwapDiagram() {
        // Only static methods, so no objects of this class are made.
    }

    /** The picture of one swap: both places, both amounts and dates, and the spoken sentences. */
    @NonNull
    public static Content of(@NonNull Context context, @NonNull RuleRunner.ShownSwap swap) {
        BatchInfo kit = swap.getKitBatch();
        BatchInfo pantry = swap.getPantryBatch();
        String forReview = context.getString(R.string.swap_diagram_for_review);
        CharSequence outDetails = highlightForReview(context,
                context.getString(R.string.swap_diagram_details_for_review,
                        BatchText.amount(context, swap.getQuantity(), kit.getUnit()),
                        BatchText.dateEntry(context, kit), forReview),
                forReview);
        String inDetails = context.getString(R.string.swap_diagram_details,
                BatchText.amount(context, swap.getQuantity(), pantry.getUnit()),
                BatchText.dateEntry(context, pantry));
        String spoken = SwapText.moveOut(context, swap) + " " + SwapText.moveIn(context, swap);
        return new Content(kit.getLocationName(), kit.getLocationType(), pantry.getLocationName(),
                pantry.getLocationType(), outDetails, inDetails, spoken);
    }

    /** Fills in a swap picture (the root of view_swap_diagram.xml). */
    public static void bind(@NonNull View diagram, @NonNull Content content) {
        place(diagram.findViewById(R.id.diagram_out_from), content.kitPlace, content.kitType);
        place(diagram.findViewById(R.id.diagram_out_to), content.pantryPlace, content.pantryType);
        place(diagram.findViewById(R.id.diagram_in_from), content.pantryPlace, content.pantryType);
        place(diagram.findViewById(R.id.diagram_in_to), content.kitPlace, content.kitType);
        ((TextView) diagram.findViewById(R.id.diagram_out_details)).setText(content.outDetails);
        ((TextView) diagram.findViewById(R.id.diagram_in_details)).setText(content.inDetails);
        diagram.setContentDescription(content.spoken);

        // With large text a move's two places go under each other, so a long place name like
        // "Earthquake kit" is never split inside a word.
        boolean stacked = LargeText.stackRows(diagram.getResources());
        arrange(diagram.findViewById(R.id.diagram_out_row),
                diagram.findViewById(R.id.diagram_out_arrow), stacked);
        arrange(diagram.findViewById(R.id.diagram_in_row),
                diagram.findViewById(R.id.diagram_in_arrow), stacked);
    }

    /**
     * Puts a move's two places side by side with a → arrow between them, or under each other
     * with a ↓ arrow.
     */
    private static void arrange(@NonNull LinearLayout row, @NonNull View arrow, boolean stacked) {
        row.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        int gap = row.getResources().getDimensionPixelSize(R.dimen.space_small);
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            if (child == arrow) {
                params.gravity = stacked ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL;
                params.setMarginStart(stacked ? 0 : gap);
                params.setMarginEnd(stacked ? 0 : gap);
                params.topMargin = stacked ? gap / 2 : 0;
                params.bottomMargin = stacked ? gap / 2 : 0;
            } else {
                params.width = stacked ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
                params.weight = stacked ? 0 : 1;
            }
            child.setLayoutParams(params);
        }
        // The arrow points down when the places are under each other. In right-to-left
        // languages the arrow itself is mirrored, so it turns the other way.
        boolean rightToLeft = row.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
        arrow.setRotation(stacked ? (rightToLeft ? -90 : 90) : 0);
    }

    /**
     * The kit's details line with its last words, "for review", in dark brass and medium weight
     * (B1). The words are the same; only their look changes, so TalkBack reads the same text.
     */
    @NonNull
    private static CharSequence highlightForReview(@NonNull Context context,
                                                   @NonNull String details,
                                                   @NonNull String forReview) {
        SpannableString styled = new SpannableString(details);
        int start = details.lastIndexOf(forReview);
        if (start >= 0) {
            int end = start + forReview.length();
            styled.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context,
                    R.color.brass_dark)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new TypefaceSpan("sans-serif-medium"), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return styled;
    }

    /** One place: its type's icon (gray, as in B1) and its name. */
    private static void place(@NonNull TextView pill, @NonNull String name,
                              @NonNull LocationType type) {
        pill.setText(name);
        StatusLook.setStartIcon(pill, PlaceLook.icon(type), MaterialColors.getColor(pill,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
    }
}

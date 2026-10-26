package com.example.readyshelf.ui.common;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import java.util.List;

/** Small helpers for drop-down lists (Spinner). */
public final class Spinners {

    private Spinners() {
        // Only static methods, so no objects of this class are made.
    }

    /** Called with the position of the item the household chose. */
    public interface OnChoice {
        void onChoice(int position);
    }

    /** Fills a drop-down list with labels and selects one of them. */
    public static void setChoices(@NonNull Spinner spinner, @NonNull List<String> labels,
                                  int selectedPosition) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedPosition, false);
    }

    /**
     * Calls {@code listener} when an item is chosen. Android also calls it once when the
     * list first appears, so the listener should ignore a choice that didn't change.
     */
    public static void setOnChoice(@NonNull Spinner spinner, @NonNull OnChoice listener) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listener.onChoice(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do.
            }
        });
    }
}

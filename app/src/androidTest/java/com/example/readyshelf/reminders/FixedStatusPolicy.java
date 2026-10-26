package com.example.readyshelf.reminders;

import androidx.annotation.NonNull;

import com.example.readyshelf.domain.BatchInfo;
import com.example.readyshelf.domain.DateInput;
import com.example.readyshelf.domain.DatePolicyContract;
import com.example.readyshelf.domain.DateStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns the status each test sets up for a batch's date input (LATER when none was set), and
 * remembers the days it was asked about. It is not a DatePolicy: it contains no date rules.
 *
 * <p>Since 2026-10-23 a shown reminder is refreshed on Reminders' own thread while the test thread
 * changes the statuses, so both methods are synchronized. Read {@code days} only in tests without
 * such refreshes.
 */
final class FixedStatusPolicy implements DatePolicyContract {

    private final Map<DateInput, DateStatus> statuses = new HashMap<>();
    final List<LocalDate> days = new ArrayList<>();

    @NonNull
    synchronized FixedStatusPolicy status(@NonNull BatchInfo batch, @NonNull DateStatus status) {
        statuses.put(DateInput.from(batch), status);
        return this;
    }

    @NonNull
    @Override
    public synchronized DateStatus statusOf(@NonNull DateInput date, @NonNull LocalDate today,
                                            int nearDateDays) {
        days.add(today);
        DateStatus status = statuses.get(date);
        return status != null ? status : DateStatus.LATER;
    }
}

package com.example.readyshelf.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Locale;

/** Tests for the date style the screens use. */
public class BatchTextTest {

    @Test
    public void date_isDayMonthWordYear() {
        assertEquals("26 Oct 2026", BatchText.date(LocalDate.of(2026, 10, 26), Locale.US));
    }

    @Test
    public void date_singleDigitDayHasNoLeadingZero() {
        assertEquals("4 Mar 2027", BatchText.date(LocalDate.of(2027, 3, 4), Locale.US));
    }

    @Test
    public void date_theLeapDay() {
        assertEquals("29 Feb 2028", BatchText.date(LocalDate.of(2028, 2, 29), Locale.US));
    }

    @Test
    public void productDetails_leavesOutEmptyParts() {
        assertEquals("Low sodium · 15 oz", BatchText.productDetails("Low sodium", "15 oz"));
        assertEquals("15 oz", BatchText.productDetails("", "15 oz"));
        assertEquals("", BatchText.productDetails("", ""));
    }
}

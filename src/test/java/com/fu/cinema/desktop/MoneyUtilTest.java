package com.fu.cinema.desktop;

import com.fu.cinema.desktop.util.MoneyUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class MoneyUtilTest {
    @Test void formatsNullZeroNegativeAndLargeAmounts() {
        assertEquals("0 đ", MoneyUtil.format(null));
        assertEquals("0 đ", MoneyUtil.format(BigDecimal.ZERO));
        assertEquals("-1.234 đ", MoneyUtil.format(new BigDecimal("-1234")));
        assertEquals("1.234.567.890.123.456.789 đ", MoneyUtil.format(new BigDecimal("1234567890123456789")));
    }
    @Test void parsesVietnameseCurrencyWithoutLosingPrecision() {
        assertEquals(new BigDecimal("1234567.50"), MoneyUtil.parse("1.234.567,50 đ"));
        assertEquals(new BigDecimal("-1200"), MoneyUtil.parse("-1.200 ₫"));
        assertEquals(new BigDecimal("1000"), MoneyUtil.parse("1000"));
    }
    @Test void rejectsInvalidOrAmbiguousInput() {
        for (String value : new String[]{"", "12.34", "1,2,3", "NaN", "1e9", "1 000"})
            assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parse(value), value);
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parse(null));
    }
    @Test void concurrentFormattingIsStable() {
        IntStream.range(0, 1000).parallel().forEach(i ->
                assertEquals("1.234.567 đ", MoneyUtil.format(new BigDecimal("1234567"))));
    }
}

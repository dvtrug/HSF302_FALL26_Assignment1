package com.fu.cinema.desktop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtil {

    private MoneyUtil() {
    }

    public static String format(BigDecimal v) {
        // NumberFormat is mutable and is not safe to share between virtual threads.
        return v == null ? "0 đ" : NumberFormat.getNumberInstance(Locale.of("vi", "VN")).format(v) + " đ";
    }

    /** Accept Vietnamese grouping dots, decimal comma and an optional đ/₫ suffix. */
    public static BigDecimal parse(String value) {
        if (value == null) throw new IllegalArgumentException("Vui lòng nhập số tiền");
        String text = value.strip().replaceFirst("\\s*[đ₫]$", "").strip();
        if (!text.matches("-?(?:\\d+|\\d{1,3}(?:\\.\\d{3})+)(?:,\\d+)?")) {
            throw new IllegalArgumentException("Số tiền không đúng định dạng Việt Nam");
        }
        return new BigDecimal(text.replace(".", "").replace(',', '.'));
    }
}

package com.fu.cinema.desktop;

import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.util.MoneyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopUtilitiesTest {

    @Test
    void moneyUtil_formatsCorrectly() {
        assertEquals("0 đ", MoneyUtil.format(null));
        String formatted = MoneyUtil.format(BigDecimal.valueOf(150000));
        assertTrue(formatted.contains("150.000") && formatted.contains("đ"));
    }

    @Test
    void pageResult_mapsCorrectly() {
        List<String> raw = List.of("A", "B", "C");
        PageImpl<String> page = new PageImpl<>(raw, PageRequest.of(0, 10), 3);
        PageResult<String> result = PageResult.of(page, String::toLowerCase);

        assertEquals(3, result.items().size());
        assertEquals("a", result.items().get(0));
        assertEquals(0, result.page());
        assertEquals(1, result.totalPages());
        assertEquals(3L, result.totalElements());
    }
}

package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.MovieRevenueDTO;
import com.fu.cinema.dto.ReportResult;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.ReportService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportFacadeUnitTest {

    private final ReportService reports = mock(ReportService.class);
    private final DesktopSession session = mock(DesktopSession.class);
    private final ReportFacade facade = new ReportFacade(reports, session);
    private final LocalDate date = LocalDate.of(2026, 11, 9);

    @Test
    void checksAuthorizationBeforeReadingReportData() {
        doThrow(new BusinessException("Bạn không có quyền")).when(session).requireAdmin();

        assertThrows(BusinessException.class, () -> facade.generate(date, date));

        verifyNoInteractions(reports);
    }

    @Test
    void mapsRevenueAndTotalsFromTheSharedReportService() {
        var revenue = new BigDecimal("160000");
        when(reports.generate(date, date)).thenReturn(new ReportResult(date, date,
                List.of(new MovieRevenueDTO("Phim", 2L, revenue)), List.of(), 0, 2, revenue));

        var result = facade.generate(date, date);

        var calls = inOrder(session, reports);
        calls.verify(session).requireAdmin();
        calls.verify(reports).generate(date, date);
        assertEquals(List.of(new RevenueRow("Phim", 2, revenue)), result.revenueByMovie());
        assertEquals(revenue, result.totalRevenue());
        assertEquals(2, result.totalTickets());
    }
}

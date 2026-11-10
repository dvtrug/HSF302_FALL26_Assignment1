package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "desktop"})
class ReportFacadeTest {

    @Autowired private AuthFacade auth;
    @Autowired private DesktopSession session;
    @Autowired private ReportFacade facade;
    @Autowired private ReportService webReport;

    @BeforeEach
    void login() {
        session.login(auth.loginAdmin("admin", "Admin@123"));
    }

    @AfterEach
    void logout() {
        session.logout();
    }

    @Test
    void matchesWebReportAndMapsConfirmedBookingsOutsideTransaction() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);

        var expected = webReport.generate(from, to);
        var actual = facade.generate(from, to);

        assertFalse(actual.isEmpty());
        assertEquals(expected.getTotalBookings(), actual.totalBookings());
        assertEquals(expected.getTotalTickets(), actual.totalTickets());
        assertEquals(expected.getTotalRevenue(), actual.totalRevenue());
        assertEquals(expected.getBookings().stream().map(b -> b.getBookingId()).toList(),
                actual.bookings().stream().map(b -> b.id()).toList());
        assertEquals(actual.totalTickets(), actual.bookings().stream().mapToInt(b -> b.ticketCount()).sum());
        assertTrue(actual.bookings().stream().allMatch(b -> !b.movieTitle().isBlank() && !b.customerName().isBlank()));
    }

    @Test
    void rejectsMissingAndReversedDates() {
        LocalDate today = LocalDate.now();
        assertThrows(BusinessException.class, () -> facade.generate(null, today));
        assertThrows(BusinessException.class, () -> facade.generate(today, today.minusDays(1)));
    }

    @Test
    void returnsEmptyDataForAnUnusedPeriod() {
        LocalDate date = LocalDate.of(2001, 1, 1);
        var report = facade.generate(date, date);
        assertTrue(report.isEmpty());
        assertTrue(report.revenueByMovie().isEmpty());
        assertEquals(0, report.totalBookings());
        assertEquals(0, report.totalTickets());
    }
}

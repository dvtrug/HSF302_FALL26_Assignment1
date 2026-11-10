package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportFacade {

    private final ReportService reports;
    private final DesktopSession session;

    public ReportData generate(LocalDate from, LocalDate to) {
        session.requireAdmin();
        var report = reports.generate(from, to);
        var revenue = report.getRevenueByMovie().stream()
                .map(row -> new RevenueRow(row.getTitle(), row.getTicketCount(), row.getRevenue()))
                .toList();
        var bookings = report.getBookings().stream()
                .map(booking -> new ReportBookingRow(booking.getBookingId(), booking.getBookingDate(),
                        booking.getCustomer().getFullName(), booking.getShowtime().getMovie().getTitle(),
                        booking.getTickets().size(), booking.getTotalAmount()))
                .toList();
        return new ReportData(revenue, bookings, report.getTotalBookings(), report.getTotalTickets(),
                report.getTotalRevenue());
    }
}

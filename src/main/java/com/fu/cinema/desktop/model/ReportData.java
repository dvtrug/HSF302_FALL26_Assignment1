package com.fu.cinema.desktop.model;

import java.math.BigDecimal;
import java.util.List;

public record ReportData(List<RevenueRow> revenueByMovie, List<ReportBookingRow> bookings,
                         long totalBookings, long totalTickets, BigDecimal totalRevenue) {
    public ReportData {
        revenueByMovie = List.copyOf(revenueByMovie);
        bookings = List.copyOf(bookings);
    }

    public boolean isEmpty() {
        return bookings.isEmpty();
    }
}

package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingRow(Long id, String customerName, String movieTitle, String roomName,
                         LocalDateTime startTime, int seatCount, BigDecimal total,
                         BookingStatus status, LocalDateTime bookingDate) {
    /** Map while the facade transaction is open, including the lazy room/tickets. */
    public static BookingRow from(Booking booking) {
        var showtime = booking.getShowtime();
        return new BookingRow(booking.getBookingId(), booking.getCustomer().getFullName(),
                showtime.getMovie().getTitle(), showtime.getRoom().getRoomName(), showtime.getStartTime(),
                booking.getTickets().size(), booking.getTotalAmount(), booking.getStatus(), booking.getBookingDate());
    }
}

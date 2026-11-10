package com.fu.cinema.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportBookingRow(long id, LocalDateTime bookingDate, String customerName, String movieTitle,
                               int ticketCount, BigDecimal totalAmount) {}

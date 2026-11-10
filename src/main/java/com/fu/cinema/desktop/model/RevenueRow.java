package com.fu.cinema.desktop.model;

import java.math.BigDecimal;

public record RevenueRow(String movieTitle, long ticketCount, BigDecimal revenue) {}

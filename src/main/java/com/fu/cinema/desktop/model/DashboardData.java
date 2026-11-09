package com.fu.cinema.desktop.model;

import java.util.List;

/**
 * Record tổng hợp các chỉ số KPI và dữ liệu biểu đồ cho màn hình Dashboard.
 */
public record DashboardData(
        long showingMovies,
        long customers,
        long pendingBookings,
        List<RoomCount> todayByRoom
) {}

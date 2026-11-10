package com.fu.cinema.desktop.model;

import java.util.List;

/**
 * Record tổng hợp toàn bộ số liệu thống kê và dữ liệu biểu đồ cho màn hình Dashboard (TODO 37).
 * <p>
 * Cung cấp 3 chỉ số KPI chính và danh sách phân bổ suất chiếu hôm nay theo từng phòng.
 * Dữ liệu được nạp bên trong transaction thông qua {@code DashboardFacade}.
 * </p>
 *
 * @param showingMovies   tổng số phim đang chiếu (khớp trang {@code /admin} của web)
 * @param customers       tổng số tài khoản khách hàng trên hệ thống
 * @param pendingBookings số đơn đặt vé ở trạng thái {@code PENDING} cần nhân viên xử lý
 * @param todayByRoom     danh sách số lượng suất chiếu hôm nay theo từng phòng chiếu
 */
public record DashboardData(
        long showingMovies,
        long customers,
        long pendingBookings,
        List<RoomCount> todayByRoom
) {}

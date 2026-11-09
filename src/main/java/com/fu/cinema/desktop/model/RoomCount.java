package com.fu.cinema.desktop.model;

/**
 * Record chứa thông tin số lượng suất chiếu hôm nay của một phòng chiếu.
 * <p>
 * Được ánh xạ từ kết quả truy vấn native projection {@code RoomShowtimeCount} (Q11).
 * </p>
 *
 * @param roomName tên phòng chiếu hiển thị trên trục hoành biểu đồ
 * @param count    tổng số suất chiếu trong ngày hôm nay của phòng
 */
public record RoomCount(String roomName, long count) {}

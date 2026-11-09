package com.fu.cinema.desktop.model;

/**
 * Record rút gọn của phòng chiếu dùng cho các ComboBox chọn phòng chiếu (TODO 41a / TODO 41b).
 *
 * @param id   mã định danh phòng chiếu
 * @param name tên phòng chiếu hiển thị trên UI
 */
public record RoomItem(Long id, String name) {
    @Override
    public String toString() {
        return name;
    }
}

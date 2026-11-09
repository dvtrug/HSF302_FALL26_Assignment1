package com.fu.cinema.desktop.model;

/**
 * Record rút gọn của phòng chiếu dùng cho các ComboBox chọn phòng.
 *
 * @param id   mã định danh phòng chiếu
 * @param name tên phòng chiếu
 */
public record RoomItem(Long id, String name) {
    @Override
    public String toString() {
        return name;
    }
}

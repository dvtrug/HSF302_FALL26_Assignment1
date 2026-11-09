package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Room;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;

/**
 * Record đại diện cho một dòng hiển thị phòng chiếu trên TableView.
 *
 * @param id          mã định danh phòng chiếu
 * @param name        tên phòng chiếu
 * @param type        loại phòng chiếu (2D, 3D, IMAX)
 * @param rows        số hàng ghế (1..26)
 * @param seatsPerRow số ghế mỗi hàng (1..30)
 * @param status      trạng thái hoạt động (ACTIVE, MAINTENANCE)
 */
public record RoomRow(
        Long id,
        String name,
        RoomType type,
        int rows,
        int seatsPerRow,
        RoomStatus status
) {
    /**
     * Tính tổng sức chứa ghế của phòng chiếu.
     *
     * @return số ghế tối đa = rows * seatsPerRow
     */
    public int capacity() {
        return rows * seatsPerRow;
    }

    public static RoomRow from(Room r) {
        return new RoomRow(
                r.getRoomId(),
                r.getRoomName(),
                r.getRoomType(),
                r.getTotalRows(),
                r.getSeatsPerRow(),
                r.getStatus()
        );
    }
}

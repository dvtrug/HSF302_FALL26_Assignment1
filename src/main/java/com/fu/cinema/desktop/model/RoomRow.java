package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Room;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;

/**
 * Record đại diện cho một dòng hiển thị phòng chiếu trên TableView (TODO 41a).
 * <p>
 * Lưu giữ thông tin phòng chiếu bất biến, hỗ trợ tính toán sức chứa tức thì
 * theo công thức {@code capacity = rows * seatsPerRow}.
 * </p>
 *
 * @param id          mã định danh phòng chiếu
 * @param name        tên phòng chiếu (duy nhất, tối đa 50 ký tự)
 * @param type        loại phòng chiếu (2D, 3D, IMAX)
 * @param rows        số hàng ghế (1..26, đại diện cho các hàng từ A đến Z)
 * @param seatsPerRow số ghế trên mỗi hàng (1..30)
 * @param status      trạng thái hoạt động (ACTIVE hoặc MAINTENANCE)
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
     * Tính tổng sức chứa tối đa của phòng chiếu.
     *
     * @return số ghế tối đa = {@code rows * seatsPerRow}
     */
    public int capacity() {
        return rows * seatsPerRow;
    }

    /**
     * Phương thức tiện ích ánh xạ từ JPA Entity {@link Room} sang Record {@link RoomRow}.
     *
     * @param r đối tượng thực thể Room nạp từ CSDL
     * @return bản ghi RoomRow tương ứng để hiển thị trên JavaFX UI
     */
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

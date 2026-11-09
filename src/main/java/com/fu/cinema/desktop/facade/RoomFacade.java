package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.RoomRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Facade phụ trách các nghiệp vụ quản lý phòng chiếu trên ứng dụng Desktop (TODO 41a).
 * <p>
 * Tuân thủ nghiêm ngặt các quy tắc kỹ thuật và nghiệp vụ:
 * <ul>
 *   <li>Không inject Repository: chỉ gọi trung gian qua {@link RoomService}</li>
 *   <li>Map Entity sang Record {@link RoomRow} trong ranh giới giao dịch {@code @Transactional(readOnly = true)}</li>
 *   <li>Kiểm soát phân quyền BR-D1: Mọi thao tác ghi ({@code save}, {@code delete}) bắt buộc gọi {@code session.requireAdmin()}</li>
 *   <li>Thực thi quy tắc an toàn BR-06: Phòng chiếu đã có suất chiếu không được xoá hẳn mà tự động chuyển sang {@code MAINTENANCE}</li>
 * </ul>
 * </p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomFacade {

    private final RoomService roomService;
    private final DesktopSession session;

    /**
     * Lấy toàn bộ danh sách phòng chiếu trong hệ thống.
     *
     * @return danh sách các bản ghi {@link RoomRow} phục vụ hiển thị trên TableView
     */
    public List<RoomRow> findAll() {
        return roomService.findAll().stream()
                .map(RoomRow::from)
                .toList();
    }

    /**
     * Lấy danh sách các phòng chiếu đang hoạt động (ACTIVE), phục vụ chọn phòng khi xếp suất.
     *
     * @return danh sách các {@link RoomItem} chỉ chứa phòng hoạt động
     */
    public List<RoomItem> activeRooms() {
        return roomService.findActive().stream()
                .map(r -> new RoomItem(r.getRoomId(), r.getRoomName()))
                .toList();
    }

    /**
     * Lấy dữ liệu form của phòng chiếu để chỉnh sửa.
     *
     * @param roomId mã định danh phòng chiếu
     * @return form DTO chứa thông tin phòng
     */
    public RoomForm getForm(Long roomId) {
        return roomService.getForm(roomId);
    }

    /**
     * Thêm mới hoặc cập nhật thông tin phòng chiếu.
     *
     * @param form dữ liệu phòng chiếu từ giao diện người dùng
     * @throws com.fu.cinema.exception.BusinessException nếu phiên đăng nhập không phải ADMIN hoặc trùng tên phòng
     */
    @Transactional
    public void save(RoomForm form) {
        session.requireAdmin();
        if (form.getRoomId() == null) {
            roomService.create(form);
        } else {
            roomService.update(form.getRoomId(), form);
        }
    }

    /**
     * Xử lý xoá phòng chiếu theo quy định an toàn BR-06.
     *
     * @param roomId mã định danh phòng chiếu cần xoá
     * @return {@code true} nếu đã xoá vĩnh viễn khỏi CSDL (chưa từng có suất);
     *         {@code false} nếu phòng đã có suất chiếu và được chuyển sang trạng thái {@code MAINTENANCE}
     * @throws com.fu.cinema.exception.BusinessException nếu phiên đăng nhập không phải ADMIN
     */
    @Transactional
    public boolean delete(Long roomId) {
        session.requireAdmin();
        return roomService.delete(roomId);
    }
}

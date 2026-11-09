package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.GenreRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.entity.Genre;
import com.fu.cinema.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * Facade chịu trách nhiệm cung cấp nghiệp vụ Quản lý Thể loại cho giao diện Desktop.
 * <p>
 * Luồng hoạt động:
 * <ul>
 *   <li>Thao tác đọc dữ liệu {@link #findAll()} bọc trong read-only transaction.</li>
 *   <li>Thao tác ghi {@link #save(GenreForm)} và {@link #delete(Long)} bắt buộc phiên đăng nhập Admin (BR-D1).</li>
 *   <li>Quy tắc BR-05: Kiểm tra và ngăn chặn việc xoá thể loại khi còn phim liên kết.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GenreFacade {

    private final GenreService genreService;
    private final DesktopSession session;

    /**
     * Lấy danh sách toàn bộ thể loại để hiển thị lên TableView.
     */
    public List<GenreRow> findAll() {
        return genreService.findAll().stream()
                .map(GenreRow::from)
                .toList();
    }

    /**
     * Thêm mới hoặc cập nhật thể loại (yêu cầu quyền Admin).
     */
    @Transactional
    public void save(GenreForm form) {
        session.requireAdmin();
        if (form.getGenreId() == null) {
            genreService.create(form);
        } else {
            genreService.update(form.getGenreId(), form);
        }
    }

    /**
     * Xoá thể loại theo ID (chặn BR-05 nếu còn phim thuộc thể loại này).
     */
    @Transactional
    public void delete(Long genreId) {
        session.requireAdmin();
        genreService.delete(genreId);
    }
}
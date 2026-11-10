package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.MovieRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MovieFacadeTest {

    @Autowired
    private MovieFacade movieFacade;

    @Autowired
    private DesktopSession session;

    @BeforeEach
    void setupAdminSession() {
        session.login(SessionUser.builder()
                .accountId(1L)
                .username("admin@fucinema.vn")
                .role(Role.ADMIN)
                .displayName("Admin")
                .build());
    }

    @Test
    @DisplayName("Tìm kiếm phim và phân trang nạp dữ liệu hợp lệ")
    void testSearchAndPagination() {
        PageResult<MovieRow> res = movieFacade.search("", null, null, 0, 5);
        assertNotNull(res);
        assertFalse(res.items().isEmpty(), "Phải có phim trả về từ seed data");
        assertTrue(res.items().size() <= 5, "Số lượng phim mỗi trang tối đa là 5");

        // Kiểm tra ánh xạ thể loại nạp không bị LazyInitializationException
        MovieRow first = res.items().getFirst();
        assertNotNull(first.title());
        assertNotNull(first.genreName(), "Tên thể loại phải được lấy thành công");
    }

    @Test
    @DisplayName("Lọc phim theo trạng thái STOPPED hoặc NOW_SHOWING")
    void testFilterByStatus() {
        PageResult<MovieRow> res = movieFacade.search("", null, MovieStatus.SHOWING, 0, 10);
        assertNotNull(res);
        for (MovieRow row : res.items()) {
            assertEquals(MovieStatus.SHOWING, row.status());
        }
    }
}
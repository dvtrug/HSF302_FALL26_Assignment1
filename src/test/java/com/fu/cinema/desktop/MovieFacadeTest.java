package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.MovieRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
    @DisplayName("Lọc phim theo trạng thái STOPPED hoặc SHOWING")
    void testFilterByStatus() {
        PageResult<MovieRow> res = movieFacade.search("", null, MovieStatus.SHOWING, 0, 10);
        assertNotNull(res);
        for (MovieRow row : res.items()) {
            assertEquals(MovieStatus.SHOWING, row.status());
        }
    }

    @Test
    @DisplayName("Lưu phim mới thành công qua MovieFacade")
    void testSaveMovieSuccess() {
        MovieForm form = new MovieForm();
        form.setTitle("Phim Test Mới 2026");
        form.setDirector("Đạo diễn Test");
        form.setDuration(120);
        form.setReleaseDate(LocalDate.now());
        form.setAgeRating(AgeRating.T16);
        form.setStatus(MovieStatus.SHOWING);
        form.setGenreId(movieFacade.findAllGenres().getFirst().genreId());
        form.setPosterUrl("/images/test.jpg");
        form.setDescription("Mô tả test phim mới");

        assertDoesNotThrow(() -> movieFacade.save(form));
    }

    @Test
    @DisplayName("BR-06: Xoá phim có suất chiếu sẽ chuyển sang STOPPED")
    void testDeleteMovieWithShowtimeSoftDeletes() {
        // Phim ID 1 đã có suất chiếu từ seed data
        boolean hardDeleted = movieFacade.delete(1L);
        assertFalse(hardDeleted, "Phim có suất chiếu không được xoá hẳn, phải trả về false");
        assertEquals(MovieStatus.STOPPED, movieFacade.getForm(1L).getStatus(), "Trạng thái phải chuyển sang STOPPED");
    }
}

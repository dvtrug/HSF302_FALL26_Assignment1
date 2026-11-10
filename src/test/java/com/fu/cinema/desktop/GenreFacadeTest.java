package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.GenreFacade;
import com.fu.cinema.desktop.model.GenreRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Genre;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.GenreRepository;
import com.fu.cinema.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GenreFacadeTest {

    @Autowired
    private GenreFacade genreFacade;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private DesktopSession session;

    @BeforeEach
    void setupAdminSession() {
        session.login(SessionUser.builder()
                .accountId(1L)
                .username("admin@fucinema.vn")
                .role(Role.ADMIN)
                .displayName("Quản trị viên")
                .build());
    }

    @Test
    @DisplayName("Nạp danh sách thể loại thành công")
    void testFindAllGenres() {
        List<GenreRow> list = genreFacade.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Danh sách thể loại không được rỗng sau khi seed data");
    }

    @Test
    @DisplayName("BR-05: Chặn xoá thể loại nếu vẫn còn phim liên kết")
    void testDeleteGenreWithAttachedMoviesFails() {
        // Tìm thể loại đang có phim gắn vào
        Genre genreWithMovie = genreRepository.findAll().stream()
                .filter(g -> movieRepository.existsByGenre_GenreId(g.getGenreId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cần có ít nhất 1 thể loại có phim để test"));

        Long genreId = genreWithMovie.getGenreId();

        // Kỳ vọng quăng BusinessException khi xoá
        BusinessException ex = assertThrows(BusinessException.class, () -> genreFacade.delete(genreId));
        assertTrue(ex.getMessage().contains("Không thể xoá"), "Thông báo lỗi phải chứa thông điệp chặn xoá");
    }

    @Test
    @DisplayName("Tạo mới và xoá thành công thể loại chưa có phim")
    void testCreateAndDeleteGenreSuccess() {
        GenreForm form = new GenreForm();
        form.setGenreName("Thể Loại Test Độc Lập");
        form.setDescription("Mô tả thể loại độc lập để test xoá");

        genreFacade.save(form);

        Genre created = genreRepository.findAll().stream()
                .filter(g -> "Thể Loại Test Độc Lập".equals(g.getGenreName()))
                .findFirst()
                .orElse(null);

        assertNotNull(created);

        // Xoá thành công vì chưa có phim nào gắn vào
        assertDoesNotThrow(() -> genreFacade.delete(created.getGenreId()));
    }
}
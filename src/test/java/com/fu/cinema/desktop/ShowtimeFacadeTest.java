package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.ShowtimeFacade;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.enums.Role;
import com.fu.cinema.enums.ShowtimeStatus;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:showtimefacade;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class ShowtimeFacadeTest {

    @Autowired
    private ShowtimeFacade showtimeFacade;

    @Autowired
    private DesktopSession session;

    @BeforeEach
    void setupAdminSession() {
        session.login(SessionUser.builder()
                .accountId(1L)
                .username("admin@fucinema.vn")
                .role(Role.ADMIN)
                .displayName("Quản Trị Viên")
                .build());
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void schedulableMovies_returnsNonEmptyList() {
        List<MovieItem> movies = showtimeFacade.schedulableMovies();
        assertNotNull(movies);
        assertFalse(movies.isEmpty(), "Danh sách phim có thể xếp lịch không được rỗng");
    }

    @Test
    void activeRooms_excludesMaintenanceRooms() {
        List<RoomItem> rooms = showtimeFacade.activeRooms();
        assertNotNull(rooms);
        assertFalse(rooms.isEmpty());
        // Verify room 3 in maintenance is excluded
        assertTrue(rooms.stream().noneMatch(r -> r.name().equalsIgnoreCase("Room 03")));
    }

    @Test
    void search_returnsPagedShowtimes() {
        PageResult<ShowtimeRow> result = showtimeFacade.search(null, null, null, 0);
        assertNotNull(result);
        assertNotNull(result.items());
        assertFalse(result.items().isEmpty(), "Phải có suất chiếu từ data seeder");
    }

    @Test
    void save_whenScheduleConflict_shouldThrowBusinessException() {
        List<MovieItem> movies = showtimeFacade.schedulableMovies();
        List<RoomItem> rooms = showtimeFacade.activeRooms();
        assertFalse(movies.isEmpty());
        assertFalse(rooms.isEmpty());

        Long movieId = movies.get(0).id();
        Long roomId = rooms.get(0).id();
        LocalDate futureDate = LocalDate.now().plusDays(10);

        // Suất 1: 14:00 -> 16:30
        ShowtimeForm form1 = new ShowtimeForm();
        form1.setMovieId(movieId);
        form1.setRoomId(roomId);
        form1.setStartTime(futureDate.atTime(14, 0));
        form1.setEndTime(futureDate.atTime(16, 30));
        form1.setTicketPrice(new BigDecimal("80000"));
        form1.setStatus(ShowtimeStatus.OPEN);
        showtimeFacade.save(form1);

        // Suất 2: Trùng giờ tại cùng phòng chiếu (15:00 -> 17:00)
        ShowtimeForm form2 = new ShowtimeForm();
        form2.setMovieId(movieId);
        form2.setRoomId(roomId);
        form2.setStartTime(futureDate.atTime(15, 0));
        form2.setEndTime(futureDate.atTime(17, 0));
        form2.setTicketPrice(new BigDecimal("85000"));
        form2.setStatus(ShowtimeStatus.OPEN);

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeFacade.save(form2));
        assertTrue(ex.getMessage().contains("Trùng lịch"), "Thông báo lỗi phải thể hiện trùng lịch chiếu (BR-08)");
    }

    @Test
    void save_whenNotAdmin_shouldThrowBusinessException() {
        session.logout(); // Unauthenticated or non-admin

        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(1L);
        form.setRoomId(1L);
        form.setStartTime(LocalDateTime.now().plusDays(5));
        form.setTicketPrice(new BigDecimal("75000"));

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeFacade.save(form));
        assertTrue(ex.getMessage().contains("không có quyền"));
    }
}

package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.RoomFacade;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.RoomRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:roomfacade;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class RoomFacadeTest {

    @Autowired
    private RoomFacade roomFacade;

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
    void roomRow_capacityCalculatesRowsTimesSeatsPerRow() {
        RoomRow row = new RoomRow(1L, "Phòng 1", RoomType.TWO_D, 10, 12, RoomStatus.ACTIVE);
        assertEquals(120, row.capacity());

        RoomRow imax = new RoomRow(2L, "IMAX 01", RoomType.IMAX, 15, 20, RoomStatus.ACTIVE);
        assertEquals(300, imax.capacity());
    }

    @Test
    void findAll_returnsSeededRooms() {
        List<RoomRow> rooms = roomFacade.findAll();
        assertNotNull(rooms);
        assertFalse(rooms.isEmpty(), "Danh sách phòng chiếu không được rỗng sau khi seed");
        assertTrue(rooms.stream().allMatch(r -> r.capacity() == r.rows() * r.seatsPerRow()));
    }

    @Test
    void activeRooms_returnsOnlyActiveRooms() {
        List<RoomItem> active = roomFacade.activeRooms();
        assertNotNull(active);
        assertFalse(active.isEmpty());
    }

    @Test
    void writeOperation_requiresAdminSession() {
        session.logout();

        RoomForm form = new RoomForm();
        form.setRoomName("Phòng Thử Nghiệm");
        form.setRoomType(RoomType.TWO_D);
        form.setTotalRows(8);
        form.setSeatsPerRow(10);
        form.setStatus(RoomStatus.ACTIVE);

        assertThrows(BusinessException.class, () -> roomFacade.save(form));
        assertThrows(BusinessException.class, () -> roomFacade.delete(1L));
    }
}

package com.fu.cinema.service;

import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.entity.Room;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.RoomRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomServiceUnitTest {

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    private final RoomService service = new RoomServiceImpl(rooms, showtimes);

    @Test
    void createsRoomWithTrimmedNameAndRequestedLayout() {
        service.create(form());

        verify(rooms).save(argThat(room ->
                "Phòng 2".equals(room.getRoomName()) && room.getTotalRows() == 8
                        && room.getSeatsPerRow() == 12 && room.getStatus() == RoomStatus.ACTIVE));
    }

    @Test
    void rejectsDuplicateRoomNameBeforeSaving() {
        when(rooms.existsByRoomNameIgnoreCase("Phòng 2")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(form()));
        verify(rooms, never()).save(any());
    }

    @Test
    void preventsLayoutChangesForRoomWithShowtimes() {
        var room = existingRoom();
        when(rooms.findById(1L)).thenReturn(Optional.of(room));
        when(showtimes.existsByRoom_RoomId(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.update(1L, form()));
        assertEquals(6, room.getTotalRows());
        assertEquals("Phòng 1", room.getRoomName());
    }

    @Test
    void updatesUnusedRoomAndMapsAllEditableFields() {
        var room = existingRoom();
        room.setRoomId(1L);
        when(rooms.findById(1L)).thenReturn(Optional.of(room));

        service.update(1L, form());

        var mapped = service.getForm(1L);
        assertEquals(1L, mapped.getRoomId());
        assertEquals("Phòng 2", mapped.getRoomName());
        assertEquals(RoomType.THREE_D, mapped.getRoomType());
        assertEquals(8, mapped.getTotalRows());
        assertEquals(12, mapped.getSeatsPerRow());
        assertEquals(RoomStatus.ACTIVE, mapped.getStatus());
    }

    @Test
    void movesScheduledRoomToMaintenanceAndDeletesUnusedRoom() {
        var scheduled = existingRoom();
        var unused = existingRoom();
        when(rooms.findById(1L)).thenReturn(Optional.of(scheduled));
        when(rooms.findById(2L)).thenReturn(Optional.of(unused));
        when(showtimes.existsByRoom_RoomId(1L)).thenReturn(true);

        assertFalse(service.delete(1L));
        assertEquals(RoomStatus.MAINTENANCE, scheduled.getStatus());
        verify(rooms, never()).delete(scheduled);
        assertTrue(service.delete(2L));
        verify(rooms).delete(unused);
    }

    @Test
    void listsOnlyActiveRoomsForScheduling() {
        var active = List.of(existingRoom());
        when(rooms.findByStatusOrderByRoomNameAsc(RoomStatus.ACTIVE)).thenReturn(active);

        assertEquals(active, service.findActive());
    }

    private Room existingRoom() {
        return new Room("Phòng 1", RoomType.TWO_D, 6, 10, RoomStatus.ACTIVE);
    }

    private RoomForm form() {
        var form = new RoomForm();
        form.setRoomName(" Phòng 2 ");
        form.setRoomType(RoomType.THREE_D);
        form.setTotalRows(8);
        form.setSeatsPerRow(12);
        form.setStatus(RoomStatus.ACTIVE);
        return form;
    }
}

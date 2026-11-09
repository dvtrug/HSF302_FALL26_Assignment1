package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.RoomService;
import com.fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade bridging JavaFX Desktop controllers to the backend showtime, movie, and room services.
 * Enforces admin authorization via {@link DesktopSession#requireAdmin()} on write operations.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShowtimeFacade {

    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;
    private final DesktopSession session;

    /**
     * Searches showtimes with filter criteria and returns a paged result.
     *
     * @param movieId optional movie filter ID
     * @param roomId optional room filter ID
     * @param date optional show date filter
     * @param page zero-based page index
     * @return paged list of {@link ShowtimeRow}
     */
    public PageResult<ShowtimeRow> search(Long movieId, Long roomId, LocalDate date, int page) {
        return PageResult.of(showtimeService.search(movieId, roomId, date, page, 10), s -> new ShowtimeRow(
                s.getShowtimeId(),
                s.getMovie().getTitle(),
                s.getRoom().getRoomName(),
                s.getStartTime(),
                s.getEndTime(),
                s.getTicketPrice(),
                s.getStatus()
        ));
    }

    /**
     * Retrieves schedulable movies (excludes STOPPED status movies per BR-07).
     *
     * @return list of {@link MovieItem} available for scheduling
     */
    public List<MovieItem> schedulableMovies() {
        return movieService.findSchedulable().stream()
                .map(m -> new MovieItem(m.getMovieId(), m.getTitle(), m.getDuration(), m.getAgeRating()))
                .toList();
    }

    /**
     * Retrieves active cinema rooms (excludes MAINTENANCE status rooms).
     *
     * @return list of {@link RoomItem} available for scheduling
     */
    public List<RoomItem> activeRooms() {
        return roomService.findActive().stream()
                .map(r -> new RoomItem(r.getRoomId(), r.getRoomName()))
                .toList();
    }

    /**
     * Loads the form data for a given showtime ID.
     *
     * @param id the showtime ID
     * @return the {@link ShowtimeForm}
     */
    public ShowtimeForm getForm(Long id) {
        return showtimeService.getForm(id);
    }

    /**
     * Saves a showtime (creates or updates) after verifying admin permissions.
     *
     * @param form the showtime form data
     */
    @Transactional
    public void save(ShowtimeForm form) {
        session.requireAdmin();
        if (form.getShowtimeId() == null) {
            showtimeService.create(form);
        } else {
            showtimeService.update(form.getShowtimeId(), form);
        }
    }

    /**
     * Deletes or cancels a showtime after verifying admin permissions.
     *
     * @param id the showtime ID
     * @return true if permanently deleted, false if cancelled with orders refunded/cancelled (BR-09)
     */
    @Transactional
    public boolean delete(Long id) {
        session.requireAdmin();
        return showtimeService.delete(id);
    }
}

package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.RoomService;
import com.fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade bridging the JavaFX Desktop client to showtime management, movie scheduling, and room querying services.
 * <p>
 * <b>Architectural and Business Rules:</b>
 * <ul>
 *   <li><b>BR-D1 (Authorization):</b> All mutating operations ({@link #save(ShowtimeForm)}, {@link #delete(Long)})
 *       strictly enforce administrator privileges via {@link DesktopSession#requireAdmin()}. Non-admin sessions
 *       are rejected with a {@link BusinessException}.</li>
 *   <li><b>BR-07 (Schedulable Movies):</b> Only active movies (status {@code SHOWING} or {@code UPCOMING}) can be scheduled.
 *       Movies marked as {@code STOPPED} are filtered out via {@link #schedulableMovies()}.</li>
 *   <li><b>BR-08 (Schedule Overlap Conflict Prevention):</b> Two showtimes in the same room cannot overlap in time.
 *       The required showtime span is {@code [StartTime, EndTime]}, where {@code EndTime = StartTime + MovieDuration + 15 minutes}
 *       buffer for cinema hall cleaning. If any overlap occurs, the backend service raises a {@link BusinessException}.</li>
 *   <li><b>BR-09 (Safe Showtime Cleanup and Cancellation):</b>
 *       When deleting a showtime via {@link #delete(Long)}:
 *       <ul>
 *         <li>If the showtime has no booked tickets/orders, it is permanently purged from the database (returns {@code true}).</li>
 *         <li>If bookings already exist, the showtime cannot be deleted physically. Instead, its status transitions to
 *             {@code CANCELLED}, and all associated bookings/tickets are automatically cancelled/refunded to preserve
 *             audit trails and referential integrity (returns {@code false}).</li>
 *       </ul>
 *   </li>
 * </ul>
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
     * Searches showtimes with optional filtering criteria (movie, room, date) and pagination.
     *
     * @param movieId optional movie filter ID (null indicates all movies)
     * @param roomId optional room filter ID (null indicates all rooms)
     * @param date optional screening date filter (null indicates all dates)
     * @param page zero-based page index
     * @return a {@link PageResult} containing immutable {@link ShowtimeRow} records and pagination metadata
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
     * Retrieves all movies eligible for scheduling according to rule BR-07.
     * Excludes movies marked as {@code STOPPED}.
     *
     * @return list of {@link MovieItem} records with ID, title, duration, and age rating
     */
    public List<MovieItem> schedulableMovies() {
        return movieService.findSchedulable().stream()
                .map(m -> new MovieItem(m.getMovieId(), m.getTitle(), m.getDuration(), m.getAgeRating()))
                .toList();
    }

    /**
     * Retrieves all cinema rooms currently available for scheduling.
     * Excludes rooms marked as {@code MAINTENANCE}.
     *
     * @return list of {@link RoomItem} records representing active rooms
     */
    public List<RoomItem> activeRooms() {
        return roomService.findActive().stream()
                .map(r -> new RoomItem(r.getRoomId(), r.getRoomName()))
                .toList();
    }

    /**
     * Loads the form data for a given showtime ID to support editing in the desktop UI.
     *
     * @param id the unique showtime identifier
     * @return the {@link ShowtimeForm} containing existing showtime parameters
     */
    public ShowtimeForm getForm(Long id) {
        return showtimeService.getForm(id);
    }

    /**
     * Saves a showtime (creates a new schedule entry or updates an existing one).
     * <p>
     * Enforces administrator authorization (BR-D1) and verifies that no schedule overlap
     * occurs within the same cinema room (BR-08).
     *
     * @param form the showtime form data to persist
     * @throws BusinessException if unauthorized or if a schedule overlap is detected
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
     * Deletes or cancels a showtime according to safe cleanup rule BR-09.
     * <p>
     * Enforces administrator authorization (BR-D1). If tickets have been sold, the showtime
     * status transitions to {@code CANCELLED} and existing orders are cancelled.
     *
     * @param id the unique showtime identifier
     * @return {@code true} if permanently deleted (no tickets sold), {@code false} if cancelled with orders refunded (BR-09)
     * @throws BusinessException if unauthorized
     */
    @Transactional
    public boolean delete(Long id) {
        session.requireAdmin();
        return showtimeService.delete(id);
    }
}

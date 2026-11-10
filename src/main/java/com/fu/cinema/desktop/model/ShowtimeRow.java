package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable record representing a row in the showtime management TableView.
 *
 * @param id the unique ID of the showtime
 * @param movieTitle the title of the scheduled movie
 * @param roomName the name of the room
 * @param start the start time of the showtime
 * @param end the end time of the showtime
 * @param price the base ticket price
 * @param status the current status (OPEN, CANCELLED)
 */
public record ShowtimeRow(
        Long id,
        String movieTitle,
        String roomName,
        LocalDateTime start,
        LocalDateTime end,
        BigDecimal price,
        ShowtimeStatus status
) {
}

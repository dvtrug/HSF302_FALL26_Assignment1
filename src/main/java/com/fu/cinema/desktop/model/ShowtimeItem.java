package com.fu.cinema.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ShowtimeItem(Long id, LocalDateTime start, String roomName, BigDecimal price) {
    @Override public String toString() {
        return start.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + " · " + roomName;
    }
}

package com.fu.cinema.desktop.model;

import java.util.List;
import java.util.Set;

public record SeatMapData(List<String> rowLabels, int seatsPerRow, Set<String> soldSeats) {
    public SeatMapData {
        rowLabels = List.copyOf(rowLabels);
        soldSeats = Set.copyOf(soldSeats);
    }
}

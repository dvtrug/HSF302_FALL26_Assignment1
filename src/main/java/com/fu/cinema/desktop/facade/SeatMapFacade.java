package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatMapFacade {

    private final ShowtimeService showtimes;

    public SeatMapData load(Long showtimeId) {
        var map = showtimes.getSeatMap(showtimeId);
        return new SeatMapData(map.getRowLabels(), map.getSeatsPerRow(), map.getSoldSeats());
    }
}

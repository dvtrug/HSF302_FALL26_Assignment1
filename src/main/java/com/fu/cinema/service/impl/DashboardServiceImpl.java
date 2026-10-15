package com.fu.cinema.service.impl;

import com.fu.cinema.dto.DashboardStats;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.MovieRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    private final MovieRepository movies;
    private final CustomerRepository customers;
    private final BookingRepository bookings;
    private final ShowtimeRepository showtimes;

    @Override
    public DashboardStats getStats() {
        return new DashboardStats(movies.countByStatus(MovieStatus.SHOWING),
                customers.count(), bookings.countByStatus(BookingStatus.PENDING),
                showtimes.countTodayShowtimesByRoom());
    }
}

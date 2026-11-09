package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.CounterMovieItem;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.model.SaleResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.ShowtimeService;
import com.fu.cinema.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CounterSaleFacade {
    private final CustomerService customers;
    private final MovieService movies;
    private final ShowtimeService showtimes;
    private final BookingService bookings;
    private final DesktopSession session;

    public List<CustomerItem> searchCustomers(String keyword) {
        return customers.search(keyword, 0, 20).map(c -> new CustomerItem(c.getCustomerId(), c.getFullName(),
                c.getMobile(), Period.between(c.getBirthday(), LocalDate.now()).getYears())).getContent();
    }

    public List<CounterMovieItem> movies() {
        return movies.findSchedulable().stream().map(m -> new CounterMovieItem(m.getMovieId(), m.getTitle(), m.getDuration(), m.getAgeRating())).toList();
    }

    public List<ShowtimeItem> showtimes(Long movieId) {
        return showtimes.findUpcomingByMovie(movieId).stream().map(s -> new ShowtimeItem(s.getShowtimeId(),
                s.getStartTime(), s.getRoom().getRoomName(), s.getTicketPrice())).toList();
    }
}

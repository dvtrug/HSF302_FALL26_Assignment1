package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.BookingFacade;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BookingFacadeTest {
    @Autowired BookingFacade facade;
    @Autowired DesktopSession session;
    @Autowired BookingRepository bookings;
    @Autowired CustomerRepository customers;
    @Autowired ShowtimeRepository showtimes;
    @Autowired PlatformTransactionManager tx;
    Long id;
    final LocalDate day = LocalDate.of(2026, 11, 9);

    @BeforeEach void prepare() {
        session.login(SessionUser.builder().accountId(1L).role(Role.ADMIN).displayName("Test Admin").build());
        new TransactionTemplate(tx).executeWithoutResult(status -> {
            bookings.deleteAll(); bookings.flush();
            var customer = customers.findAll().getFirst(); var showtime = showtimes.findAll().getFirst();
            for (int i=0; i<16; i++) {
                Booking booking = new Booking(); booking.setCustomer(customer); booking.setShowtime(showtime);
                booking.setStatus(BookingStatus.PENDING); booking.setBookingDate(day.atStartOfDay().plusHours(i));
                booking.setTotalAmount(showtime.getTicketPrice());
                booking.addTicket(new Ticket(showtime, "A"+(i+1), showtime.getTicketPrice()));
                bookings.save(booking); id=booking.getBookingId();
            }
        });
    }
    @Test void filtersPaginatesAndMapsOutsideTransaction() {
        var page = facade.search(BookingStatus.PENDING, day, day, null, 0);
        assertEquals(16, page.totalElements()); assertEquals(15, page.items().size()); assertEquals(2, page.totalPages());
        assertEquals(id, page.items().getFirst().id());
        assertNotNull(page.items().getFirst().roomName()); assertEquals(1, page.items().getFirst().seatCount());
        assertEquals(1, facade.search(null, day, day, page.items().getFirst().customerName(), 1).items().size());
        assertTrue(facade.search(BookingStatus.CANCELLED, day, day, null, 0).items().isEmpty());
        assertThrows(BusinessException.class, () -> facade.search(null, day.plusDays(1), day, null, 0));
        var detail=facade.detail(id); assertFalse(detail.seats().isEmpty()); assertNotNull(detail.movieTitle());
        assertThrows(UnsupportedOperationException.class, () -> detail.seats().add("Z1"));
    }
    @Test void enforcesStateTransitionsAndAdminPermission() {
        facade.confirm(id); assertEquals(BookingStatus.CONFIRMED, facade.detail(id).status());
        assertThrows(BusinessException.class, () -> facade.confirm(id));
        facade.cancel(id); assertEquals(BookingStatus.CANCELLED, facade.detail(id).status());
        assertThrows(BusinessException.class, () -> facade.confirm(id));
        assertThrows(BusinessException.class, () -> facade.cancel(id));
        session.logout();
        assertThrows(BusinessException.class, () -> facade.confirm(id));
        assertThrows(BusinessException.class, () -> facade.cancel(id));
    }
    @Test void pendingCanCancelWithoutConfirming() {
        facade.cancel(id); assertEquals(BookingStatus.CANCELLED, facade.detail(id).status());
    }
    @Test void customerCanViewOwnBookingsAndCancelFutureShowtime() {
        var customer = customers.findAll().getFirst();
        new TransactionTemplate(tx).executeWithoutResult(status -> {
            var booking = bookings.findById(id).orElseThrow();
            var st = booking.getShowtime();
            st.setStartTime(java.time.LocalDateTime.now().plusDays(2));
            st.setEndTime(st.getStartTime().plusHours(2));
            showtimes.save(st);
        });
        session.login(SessionUser.builder().accountId(customer.getAccount().getAccountId()).role(Role.CUSTOMER).displayName(customer.getFullName()).build());
        var res = facade.search(BookingStatus.PENDING, null, null, null, 0);
        assertFalse(res.items().isEmpty());
        facade.cancel(id);
        assertEquals(BookingStatus.CANCELLED, facade.detail(id).status());
    }
}

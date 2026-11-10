package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.CounterSaleFacade;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.*;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.*;
import com.fu.cinema.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.annotation.DirtiesContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:countersalefacade;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CounterSaleFacadeTest {
    @Autowired CounterSaleFacade facade;
    @Autowired DesktopSession session;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;
    @Autowired CustomerRepository customers;
    @Autowired ShowtimeRepository showtimes;
    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired PlatformTransactionManager tx;
    @MockitoSpyBean BookingService bookingService;
    Long customerId, showtimeId, minorId;

    @BeforeEach void prepare() {
        session.login(SessionUser.builder().accountId(1L).role(Role.ADMIN).displayName("Test Admin").build());
        new TransactionTemplate(tx).executeWithoutResult(status -> {
            customerId=customers.findAll().stream().filter(c -> c.getFullName().contains("An")).findFirst().orElseThrow().getCustomerId();
            minorId=customers.findAll().stream().filter(c -> c.getFullName().contains("Binh")).findFirst().orElseThrow().getCustomerId();
            Showtime showtime=new Showtime();
            showtime.setMovie(movies.findAll().stream().filter(m -> m.getAgeRating()==AgeRating.T18).findFirst().orElseThrow());
            showtime.setRoom(rooms.findAll().getFirst()); showtime.setStartTime(LocalDateTime.now().plusDays(2));
            showtime.setEndTime(showtime.getStartTime().plusHours(2)); showtime.setStatus(ShowtimeStatus.OPEN);
            showtime.setTicketPrice(new BigDecimal("95000")); showtimeId=showtimes.save(showtime).getShowtimeId();
        });
    }
    @Test void mapsSearchAndShowtimeChoicesToDetachedRecords() {
        var customer=facade.searchCustomers("An").stream().filter(c -> c.id().equals(customerId)).findFirst().orElseThrow();
        assertTrue(customer.age()>=18); assertNotNull(customer.mobile());
        assertFalse(facade.movies().isEmpty());
        var movieId=new TransactionTemplate(tx).execute(s -> showtimes.findById(showtimeId).orElseThrow().getMovie().getMovieId());
        assertTrue(facade.showtimes(movieId).stream().anyMatch(s -> s.id().equals(showtimeId)));
    }
    @Test void paidSaleConfirmsAndUnpaidRemainsPending() {
        var paid=facade.sell(customerId,showtimeId,List.of("A1","A2","A3"),true);
        assertEquals(BookingStatus.CONFIRMED,paid.status()); assertEquals(new BigDecimal("285000"),paid.total());
        assertEquals(3,paid.seats().size());
        assertEquals(BookingStatus.CONFIRMED,bookingService.getDetail(paid.bookingId()).getStatus());
        var unpaid=facade.sell(customerId,showtimeId,List.of("A4"),false);
        assertEquals(BookingStatus.PENDING,unpaid.status());
        assertThrows(UnsupportedOperationException.class,()->unpaid.seats().add("Z1"));
    }
    @Test void ageSeatLimitAndPermissionFailuresDoNotCreateOrders() {
        long before=bookings.count();
        assertThrows(BusinessException.class,()->facade.sell(minorId,showtimeId,List.of("A1"),true));
        assertThrows(BusinessException.class,()->facade.sell(customerId,showtimeId,List.of("A1","A2","A3","A4","A5","A6","A7","A8","A9"),false));
        session.logout(); assertThrows(BusinessException.class,()->facade.sell(customerId,showtimeId,List.of("A1"),true));
        assertEquals(before,bookings.count());
    }
    @Test void customerChoicesRequireAnAdminSession() {
        session.logout();
        assertThrows(BusinessException.class, () -> facade.searchCustomers("An"));
        session.login(SessionUser.builder().accountId(99L).role(Role.CUSTOMER).displayName("Customer").build());
        assertThrows(BusinessException.class, () -> facade.searchCustomers("An"));
        session.login(SessionUser.builder().accountId(1L).role(Role.ADMIN).displayName("Admin").build());
        assertFalse(facade.searchCustomers("An").isEmpty());
    }
    @Test void failedConfirmationRollsBackNewBookingAndTickets() {
        long orders=bookings.count(), ticketCount=tickets.count();
        doThrow(new BusinessException("Confirmation failed")).when(bookingService).confirm(anyLong());
        assertThrows(BusinessException.class,()->facade.sell(customerId,showtimeId,List.of("A1"),true));
        assertEquals(orders,bookings.count()); assertEquals(ticketCount,tickets.count());
    }
    @Test void webAndCounterCannotBothPurchaseSameSeatOnH2() throws Exception {
        CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
        try(ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> counter=executor.submit(()->attempt(ready,start,()->facade.sell(customerId,showtimeId,List.of("A1"),false)));
            Future<Boolean> web=executor.submit(()->attempt(ready,start,()->{
                BookingRequest request=new BookingRequest();request.setShowtimeId(showtimeId);request.setSeatCodes(List.of("A1"));
                bookingService.create(customerId,request);
            }));
            assertTrue(ready.await(5,TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(counter.get(15,TimeUnit.SECONDS),web.get(15,TimeUnit.SECONDS));
            long active = new TransactionTemplate(tx).execute(s -> bookings.findAll().stream().filter(b -> b.getShowtime().getShowtimeId().equals(showtimeId) && b.getStatus()!=BookingStatus.CANCELLED).count());
            assertEquals(1L, active);
        }
    }
    @Test void customerCanBookSelfAndCannotBookOther() {
        var customer = customers.findById(customerId).orElseThrow();
        session.login(SessionUser.builder().accountId(customer.getAccount().getAccountId()).role(Role.CUSTOMER).displayName(customer.getFullName()).build());
        var me = facade.getCurrentCustomer();
        assertNotNull(me);
        assertEquals(customerId, me.id());
        var res = facade.sell(customerId, showtimeId, List.of("B1"), false);
        assertEquals(BookingStatus.PENDING, res.status());
        assertThrows(BusinessException.class, () -> facade.sell(minorId, showtimeId, List.of("B2"), false));
    }

    private boolean attempt(CountDownLatch ready,CountDownLatch start,Runnable action) throws Exception {
        ready.countDown();start.await();
        try{action.run();return true;}catch(BusinessException ex){assertTrue(ex.getMessage().contains("Ghế đã có người đặt"));return false;}
    }
}

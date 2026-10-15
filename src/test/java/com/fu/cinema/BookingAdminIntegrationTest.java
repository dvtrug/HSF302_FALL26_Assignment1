package com.fu.cinema;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.BookingService;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class BookingAdminIntegrationTest {
    @Autowired BookingService service;
    @Autowired BookingRepository bookings;
    @Autowired CustomerRepository customers;
    @Autowired ShowtimeRepository showtimes;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired WebApplicationContext context;

    private MockMvc mvc;
    private Long bookingId;
    private final LocalDate day = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            bookings.deleteAll();
            bookings.flush();
            var customer = customers.findAll().getFirst();
            var showtime = showtimes.findAll().getFirst();
            for (int i = 0; i < 12; i++) {
                Booking booking = new Booking();
                booking.setCustomer(customer);
                booking.setShowtime(showtime);
                booking.setBookingDate(day.atStartOfDay().plusHours(i * 2L));
                booking.setStatus(BookingStatus.PENDING);
                booking.setTotalAmount(showtime.getTicketPrice());
                booking.addTicket(new Ticket(showtime, "A" + (i + 1), showtime.getTicketPrice()));
                bookings.save(booking);
                if (i == 0) bookingId = booking.getBookingId();
            }
        });
    }

    @Test
    void filtersByStatusNameEmailAndMobile() {
        Booking detail = service.getDetail(bookingId);
        for (String keyword : List.of(detail.getCustomer().getFullName().toUpperCase(),
                detail.getCustomer().getEmail(), detail.getCustomer().getMobile())) {
            assertThat(service.search(BookingStatus.PENDING, day, day, " " + keyword + " ", 0, 10)
                    .getTotalElements()).isEqualTo(12);
        }
        assertThat(service.search(BookingStatus.CANCELLED, day, day, null, 0, 10)).isEmpty();
        assertThat(service.search(null, null, null, "no-such-customer", 0, 10)).isEmpty();
    }

    @Test
    void includesEndOfDayButExcludesNextMidnightAndSortsNewestFirst() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            Booking booking = bookings.findById(bookingId).orElseThrow();
            booking.setBookingDate(day.atTime(23, 59, 59));
        });
        var result = service.search(null, day, day, "  ", 0, 10);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getContent().getFirst().getBookingId()).isEqualTo(bookingId);
        assertThat(service.search(null, day.plusDays(1), day.plusDays(1), null, 0, 10)).isEmpty();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx ->
                bookings.findById(bookingId).orElseThrow().setBookingDate(day.plusDays(1).atStartOfDay()));
        assertThat(service.search(null, day, day, null, 0, 10).getTotalElements()).isEqualTo(11);
    }

    @Test
    void rejectsReversedDates() {
        assertThatThrownBy(() -> service.search(null, day.plusDays(1), day, null, 0, 10))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Từ ngày");
    }

    @Test
    void loadsDetailRelationsOutsideTransactionAndReturnsNotFound() {
        Booking detail = service.getDetail(bookingId);
        assertThat(Hibernate.isInitialized(detail.getCustomer())).isTrue();
        assertThat(Hibernate.isInitialized(detail.getShowtime().getMovie())).isTrue();
        assertThat(Hibernate.isInitialized(detail.getShowtime().getRoom())).isTrue();
        assertThat(Hibernate.isInitialized(detail.getTickets())).isTrue();
        assertThatThrownBy(() -> service.getDetail(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void confirmsOnlyPendingAndPersistsStatus(BookingStatus initial) {
        setStatus(initial);
        if (initial == BookingStatus.PENDING) {
            service.confirm(bookingId);
            assertThat(service.getDetail(bookingId).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        } else {
            assertThatThrownBy(() -> service.confirm(bookingId)).isInstanceOf(BusinessException.class);
            assertThat(service.getDetail(bookingId).getStatus()).isEqualTo(initial);
        }
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void cancelsOnlyPendingOrConfirmedAndPersistsStatus(BookingStatus initial) {
        setStatus(initial);
        if (initial == BookingStatus.CANCELLED) {
            assertThatThrownBy(() -> service.cancelByAdmin(bookingId)).isInstanceOf(BusinessException.class);
        } else {
            service.cancelByAdmin(bookingId);
        }
        assertThat(service.getDetail(bookingId).getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void rendersListPaginationAndDateError() throws Exception {
        String html = mvc.perform(get("/admin/bookings?status=PENDING&from=2026-10-15&to=2026-10-15&kw=Nguyen"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("page=1", "status=PENDING", "from=2026-10-15", "to=2026-10-15");
        mvc.perform(get("/admin/bookings").param("page", "1"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("bookings"));
        mvc.perform(get("/admin/bookings").param("from", day.plusDays(1).toString()).param("to", day.toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void rendersActionButtonsByStatus(BookingStatus initial) throws Exception {
        setStatus(initial);
        String html = mvc.perform(get("/admin/bookings/{id}", bookingId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        if (initial == BookingStatus.PENDING) assertThat(html).contains("/confirm");
        else assertThat(html).doesNotContain("/confirm");
        if (initial != BookingStatus.CANCELLED) assertThat(html).contains("/cancel");
        else assertThat(html).doesNotContain("/cancel");
    }

    @Test
    void postActionsRedirectWithSuccessOrBusinessError() throws Exception {
        mvc.perform(post("/admin/bookings/{id}/confirm", bookingId))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        mvc.perform(post("/admin/bookings/{id}/confirm", bookingId))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/admin/bookings/{id}/cancel", bookingId))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        mvc.perform(post("/admin/bookings/{id}/cancel", bookingId))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("error"));
        mvc.perform(get("/admin/bookings/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(post("/admin/bookings/{id}/confirm", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(post("/admin/bookings/{id}/cancel", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    private void setStatus(BookingStatus status) {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            bookings.findById(bookingId).orElseThrow().setStatus(status);
            entityManager.flush();
        });
    }
}

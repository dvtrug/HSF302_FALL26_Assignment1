package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class CustomerBookingIntegrationTest {
    @Autowired BookingService service;
    @Autowired BookingRepository bookings;
    @Autowired CustomerRepository customers;
    @Autowired ShowtimeRepository showtimes;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    private Long ownerId;
    private Long otherId;
    private Long ownBookingId;
    private Long otherBookingId;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            bookings.deleteAll();
            bookings.flush();
            var customerList = customers.findAll();
            ownerId = customerList.getFirst().getCustomerId();
            otherId = customerList.get(1).getCustomerId();
            var showtime = showtimes.findAll().getFirst();
            showtime.setStartTime(LocalDateTime.now().plusHours(4));
            showtime.setEndTime(showtime.getStartTime().plusHours(2));
            for (int i = 0; i < 13; i++) {
                Booking booking = new Booking();
                booking.setCustomer(i == 12 ? customerList.get(1) : customerList.getFirst());
                booking.setShowtime(showtime);
                booking.setBookingDate(LocalDateTime.of(2026, 10, 15, 0, 0).plusHours(i));
                booking.setStatus(i < 11 ? BookingStatus.PENDING : BookingStatus.CONFIRMED);
                booking.setTotalAmount(showtime.getTicketPrice());
                booking.addTicket(new Ticket(showtime, "A" + (i + 1), showtime.getTicketPrice()));
                bookings.save(booking);
                if (i == 0) ownBookingId = booking.getBookingId();
                if (i == 12) otherBookingId = booking.getBookingId();
            }
        });
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(get("/").session(customerSession(ownerId))).build();
    }

    private MockHttpSession customerSession(Long customerId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, SessionUser.builder()
                .accountId(customerId).customerId(customerId).role(Role.CUSTOMER)
                .displayName("Test customer").build());
        return session;
    }

    @Test
    void historyIsOwnedFilteredNewestFirstAndPaginated() throws Exception {
        var page = service.findByCustomer(ownerId, null, 0, 10);
        assertThat(page.getTotalElements()).isEqualTo(12);
        assertThat(page.getContent()).allMatch(b -> b.getCustomer().getCustomerId().equals(ownerId));
        assertThat(page.getContent()).extracting(Booking::getBookingDate).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(service.findByCustomer(ownerId, BookingStatus.PENDING, 0, 10).getTotalElements()).isEqualTo(11);
        assertThat(service.findByCustomer(ownerId, null, 1, 10).getContent()).hasSize(2);
        String html = mvc.perform(get("/customer/bookings?status=PENDING"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Lịch sử đặt vé", "status=PENDING", "page=1");
        mvc.perform(get("/customer/bookings").param("page", "1")).andExpect(status().isOk());
        String empty = mvc.perform(get("/customer/bookings").param("status", "CANCELLED"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(empty).contains("Bạn chưa có đơn đặt vé nào");
    }

    @Test
    void ignoresSubmittedCustomerIdentityForListDetailAndCancel() throws Exception {
        var result = mvc.perform(get("/customer/bookings").param("customerId", otherId.toString()))
                .andExpect(status().isOk()).andReturn();
        var page = (org.springframework.data.domain.Page<?>) result.getModelAndView().getModel().get("bookings");
        assertThat(page.getTotalElements()).isEqualTo(12);
        mvc.perform(get("/customer/bookings/{id}", otherBookingId).param("customerId", otherId.toString()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/customer/bookings/{id}/cancel", otherBookingId).param("customerId", otherId.toString()))
                .andExpect(status().isNotFound());
        assertThat(bookings.findById(otherBookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void rendersOwnDetailAndReturns404ForMissingOrForeignBooking() throws Exception {
        String html = mvc.perform(get("/customer/bookings/{id}", ownBookingId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("/cancel", "A1", "2 giờ");
        mvc.perform(get("/customer/bookings/{id}", otherBookingId)).andExpect(status().isNotFound());
        mvc.perform(get("/customer/bookings/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(post("/customer/bookings/{id}/cancel", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"PENDING", "CONFIRMED"})
    void cancelsEligibleOwnBookingAndRejectsRepeatedCancellation(BookingStatus initial) throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx ->
                bookings.findById(ownBookingId).orElseThrow().setStatus(initial));
        mvc.perform(post("/customer/bookings/{id}/cancel", ownBookingId))
                .andExpect(redirectedUrl("/customer/bookings/" + ownBookingId))
                .andExpect(flash().attributeExists("success"));
        assertThat(bookings.findById(ownBookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        String html = mvc.perform(get("/customer/bookings/{id}", ownBookingId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).doesNotContain("/cancel");
        mvc.perform(post("/customer/bookings/{id}/cancel", ownBookingId))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void refusesPastOrLessThanTwoHoursBeforeShowtime() throws Exception {
        for (int minutes : List.of(-60, 60, 119)) {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx ->
                    bookings.findById(ownBookingId).orElseThrow().getShowtime()
                            .setStartTime(LocalDateTime.now().plusMinutes(minutes)));
            mvc.perform(post("/customer/bookings/{id}/cancel", ownBookingId))
                    .andExpect(flash().attribute("error", "Chỉ được huỷ đơn trước giờ chiếu ít nhất 2 giờ"));
            assertThat(bookings.findById(ownBookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING);
        }
    }

    @Test
    void blocksAnonymousAndAdminFromAllCustomerRoutes() throws Exception {
        MockMvc unprivileged = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER, SessionUser.builder().role(Role.ADMIN).build());
        for (String path : List.of("/customer/bookings", "/customer/bookings/" + ownBookingId)) {
            unprivileged.perform(get(path)).andExpect(redirectedUrl("/login?redirect=" + path));
            unprivileged.perform(get(path).session(admin)).andExpect(redirectedUrl("/error/403"));
        }
        String cancel = "/customer/bookings/" + ownBookingId + "/cancel";
        unprivileged.perform(post(cancel)).andExpect(redirectedUrl("/login?redirect=" + cancel));
        unprivileged.perform(post(cancel).session(admin)).andExpect(redirectedUrl("/error/403"));
        assertThat(bookings.findById(ownBookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void seatBookingFlowStillRedirectsToWorkingCustomerDetail() throws Exception {
        Long showtimeId = service.getDetail(ownBookingId).getShowtime().getShowtimeId();
        mvc.perform(get("/customer/showtimes/{id}/seats", showtimeId))
                .andExpect(status().isOk()).andExpect(model().attributeExists("seatMap"));
        var result = mvc.perform(post("/customer/bookings")
                        .param("showtimeId", showtimeId.toString()).param("seatCodes", "H12"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"))
                .andReturn();
        String detailUrl = result.getResponse().getRedirectedUrl();
        assertThat(detailUrl).startsWith("/customer/bookings/");
        String html = mvc.perform(get(detailUrl)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("H12", "PENDING", "/cancel");
        assertThat(service.findByCustomer(ownerId, null, 0, 10).getTotalElements()).isEqualTo(13);
    }
}

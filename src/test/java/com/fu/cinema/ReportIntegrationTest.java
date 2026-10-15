package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.enums.ShowtimeStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.DashboardService;
import com.fu.cinema.service.ReportService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportIntegrationTest {
    @Autowired ReportService reports;
    @Autowired DashboardService dashboard;
    @Autowired BookingRepository bookings;
    @Autowired CustomerRepository customers;
    @Autowired ShowtimeRepository showtimes;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    private final LocalDate day = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void setUp() {
        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER,
                SessionUser.builder().accountId(1L).displayName("Admin").role(Role.ADMIN).build());
        mvc = MockMvcBuilders.webAppContextSetup(context).defaultRequest(get("/").session(admin)).build();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            bookings.deleteAll();
            bookings.flush();
            addBooking(0, day.atStartOfDay(), BookingStatus.CONFIRMED, "100", 2);
            addBooking(1, day.atTime(23, 59, 59), BookingStatus.CONFIRMED, "300", 1);
            addBooking(0, day.plusDays(1).atStartOfDay(), BookingStatus.CONFIRMED, "900", 1);
            addBooking(0, day.atTime(12, 0), BookingStatus.PENDING, "1000", 1);
            addBooking(0, day.atTime(12, 0), BookingStatus.CANCELLED, "1000", 1);
        });
    }

    private void addBooking(int showtimeIndex, LocalDateTime date, BookingStatus status, String price, int seats) {
        var showtime = showtimes.findAll().get(showtimeIndex);
        Booking booking = new Booking();
        booking.setCustomer(customers.findAll().getFirst());
        booking.setShowtime(showtime);
        booking.setBookingDate(date);
        booking.setStatus(status);
        booking.setTotalAmount(new BigDecimal(price).multiply(BigDecimal.valueOf(seats)));
        for (int i = 1; i <= seats; i++) booking.addTicket(new Ticket(showtime, "A" + i, new BigDecimal(price)));
        bookings.save(booking);
    }

    @Test
    void reportsOnlyConfirmedIncludesWholeEndDayAndSortsBothTables() {
        var report = reports.generate(day, day);
        assertThat(report.getTotalBookings()).isEqualTo(2);
        assertThat(report.getTotalTickets()).isEqualTo(3);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("500");
        assertThat(report.getRevenueByMovie()).hasSize(2);
        assertThat(report.getRevenueByMovie().getFirst().getRevenue()).isEqualByComparingTo("300");
        assertThat(report.getBookings().getFirst().getBookingDate()).isEqualTo(day.atTime(23, 59, 59));
        assertThat(report.getBookings()).allMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);
        assertThat(report.getBookings().getFirst().getCustomer().getFullName()).isNotBlank();
        assertThat(report.getBookings().getFirst().getShowtime().getMovie().getTitle()).isNotBlank();
    }

    @Test
    void emptyPeriodHasZeroTotals() {
        var result = reports.generate(day.minusDays(2), day.minusDays(1));
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalBookings()).isZero();
        assertThat(result.getTotalTickets()).isZero();
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void validatesMissingAndReversedDates() {
        assertThatThrownBy(() -> reports.generate(null, day)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reports.generate(day, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reports.generate(day.plusDays(1), day)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rendersReportAndKeepsInvalidInputWithError() throws Exception {
        String html = mvc.perform(get("/admin/reports").param("startDate", day.toString()).param("endDate", day.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Tổng doanh thu", "500", "2026-10-15", "Doanh thu theo phim");
        mvc.perform(get("/admin/reports").param("startDate", day.plusDays(1).toString()).param("endDate", day.toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
                .andExpect(model().attributeDoesNotExist("report"));
        mvc.perform(get("/admin/reports"))
                .andExpect(status().isOk()).andExpect(model().attribute("startDate", LocalDate.now().withDayOfMonth(1)))
                .andExpect(model().attribute("endDate", LocalDate.now()));
    }

    @Test
    void dashboardCountsOpenShowtimesTodayAndIncludesEmptyRooms() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var all = showtimes.findAll();
            all.forEach(s -> s.setStartTime(LocalDate.now().minusDays(10).atTime(9, 0)));
            all.getFirst().setStartTime(LocalDate.now().atTime(9, 0));
            all.getFirst().setStatus(ShowtimeStatus.OPEN);
            all.get(1).setStartTime(LocalDate.now().atTime(12, 0));
            all.get(1).setStatus(ShowtimeStatus.CANCELLED);
        });
        var stats = dashboard.getStats();
        assertThat(stats.getShowingMovies()).isEqualTo(3);
        assertThat(stats.getCustomers()).isEqualTo(2);
        assertThat(stats.getPendingBookings()).isEqualTo(1);
        assertThat(stats.getTodayShowtimesByRoom()).hasSize(3);
        assertThat(stats.getTodayShowtimesByRoom().stream().mapToLong(r -> r.getShowtimeCount()).sum()).isEqualTo(1);
        mvc.perform(get("/admin")).andExpect(status().isOk()).andExpect(model().attributeExists("stats"));
    }

    @Test
    void protectsReportAndDashboardFromAnonymousAndCustomer() throws Exception {
        MockMvc unauthenticated = MockMvcBuilders.webAppContextSetup(context).build();
        for (String path : new String[]{"/admin", "/admin/reports"}) {
            unauthenticated.perform(get(path)).andExpect(redirectedUrl("/login?redirect=" + path));
            MockHttpSession customer = new MockHttpSession();
            customer.setAttribute(SessionUtils.CURRENT_USER, SessionUser.builder().role(Role.CUSTOMER).build());
            unauthenticated.perform(get(path).session(customer)).andExpect(redirectedUrl("/error/403"));
        }
    }
}

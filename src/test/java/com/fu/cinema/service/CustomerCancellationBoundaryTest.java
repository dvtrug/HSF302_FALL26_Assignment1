package com.fu.cinema.service;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.service.impl.BookingServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerCancellationBoundaryTest {
    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void enforcesTwoHourBoundaryIncludingEquality(long nanosAfterCutoff) {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 15, 12, 0);
        LocalDateTime now = cutoff.plusNanos(nanosAfterCutoff);
        Showtime showtime = new Showtime();
        showtime.setStartTime(cutoff.plusHours(2));
        Customer owner = new Customer();
        owner.setCustomerId(1L);
        Booking booking = new Booking();
        booking.setCustomer(owner);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        BookingRepository repository = mock(BookingRepository.class);
        when(repository.findDetailById(10L)).thenReturn(Optional.of(booking));
        BookingService service = new BookingServiceImpl(repository, null, null, null);
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(now);
            if (nanosAfterCutoff <= 0) {
                service.cancelByCustomer(10L, 1L);
                assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            } else {
                assertThatThrownBy(() -> service.cancelByCustomer(10L, 1L))
                        .isInstanceOf(BusinessException.class).hasMessageContaining("2 giờ");
                assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
            }
        }
    }
}

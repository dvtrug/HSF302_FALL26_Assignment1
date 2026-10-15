package com.fu.cinema.service;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookingService {

    private final BookingRepository bookingRepository;

    public Page<Booking> search(
            BookingStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            int page,
            int size) {

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new BusinessException(
                    "Từ ngày phải trước hoặc bằng đến ngày");
        }

        return bookingRepository.search(
                status,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay(),
                ValidationUtils.blankToNull(keyword),
                PageRequest.of(
                        Math.max(page, 0),
                        size,
                        Sort.by("bookingDate").descending()));
    }

    public Booking getDetail(Long bookingId) {
        return bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Đơn đặt vé", bookingId));
    }
}
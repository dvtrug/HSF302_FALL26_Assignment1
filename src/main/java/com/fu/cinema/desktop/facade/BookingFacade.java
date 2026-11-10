package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.BookingDetail;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingFacade {
    private final BookingService bookingService;
    private final DesktopSession session;
    private final CustomerRepository customerRepository;

    public PageResult<BookingRow> search(BookingStatus status, LocalDate from, LocalDate to, String keyword, int page) {
        SessionUser u = session.getUser();
        if (u != null && u.isCustomer()) {
            Long customerId = customerRepository.findByAccount_AccountId(u.getAccountId())
                    .map(Customer::getCustomerId).orElse(null);
            if (customerId != null) {
                return PageResult.of(bookingService.findByCustomer(customerId, status, page, 15), BookingRow::from);
            }
        }
        return PageResult.of(bookingService.search(status, from, to, keyword, page, 15), BookingRow::from);
    }

    public BookingDetail detail(Long id) {
        var booking = bookingService.getDetail(id);
        var showtime = booking.getShowtime();
        return new BookingDetail(id, booking.getCustomer().getFullName(), booking.getCustomer().getMobile(),
                showtime.getMovie().getTitle(), showtime.getRoom().getRoomName(), showtime.getStartTime(),
                booking.getTickets().stream().map(Ticket::getSeatCode).sorted().toList(),
                booking.getTotalAmount(), booking.getStatus(), booking.getBookingDate());
    }

    @Transactional
    public void confirm(Long id) {
        session.requireAdmin();
        bookingService.confirm(id);
    }

    @Transactional
    public void cancel(Long id) {
        SessionUser u = session.getUser();
        if (u == null) throw new BusinessException("Chưa đăng nhập");
        if (u.isCustomer()) {
            Long customerId = customerRepository.findByAccount_AccountId(u.getAccountId())
                    .map(Customer::getCustomerId).orElseThrow(() -> new BusinessException("Không tìm thấy khách hàng"));
            bookingService.cancelByCustomer(id, customerId);
        } else {
            session.requireAdmin();
            bookingService.cancelByAdmin(id);
        }
    }
}

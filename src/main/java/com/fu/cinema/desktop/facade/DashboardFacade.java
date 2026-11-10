package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.DashboardData;
import com.fu.cinema.desktop.model.RoomCount;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Facade chịu trách nhiệm tổng hợp các số liệu KPI và thống kê cho màn hình Dashboard (TODO 37).
 * <p>
 * Tuân thủ kiến trúc 3 lớp: chỉ gọi các Service của Assignment 01, không inject Repository,
 * và thực hiện ánh xạ trực tiếp các Entity / Projection sang Record {@link DashboardData}
 * trong ranh giới giao dịch {@code @Transactional(readOnly = true)}.
 * </p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardFacade {

    private final MovieService movieService;
    private final CustomerService customerService;
    private final BookingService bookingService;
    private final ShowtimeService showtimeService;

    /**
     * Tải toàn bộ chỉ số KPI và biểu đồ suất chiếu phòng trong ngày hôm nay.
     *
     * @return đối tượng {@link DashboardData} chứa số liệu khớp với trang quản trị web
     */
    public DashboardData load() {
        List<RoomCount> rooms = showtimeService.countTodayByRoom().stream()
                .map(r -> new RoomCount(r.getRoomName(), r.getShowtimeCount()))
                .toList();

        return new DashboardData(
                movieService.countShowing(),
                customerService.count(),
                bookingService.countPending(),
                rooms
        );
    }
}

package com.fu.cinema.controller.admin;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    public String list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String kw,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<Booking> bookings;
        try {
            bookings = bookingService.search(
                    status, from, to, kw, page, PageUtils.DEFAULT_SIZE);
        } catch (BusinessException ex) {
            bookings = Page.empty();
            model.addAttribute("error", ex.getMessage());
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("kw", kw);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());

        return "admin/bookings/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("booking", bookingService.getDetail(id));
        return "admin/bookings/detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            bookingService.confirm(id);
            attributes.addFlashAttribute("success", "Đã xác nhận đơn #" + id);
        } catch (BusinessException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            bookingService.cancelByAdmin(id);
            attributes.addFlashAttribute("success", "Đã huỷ đơn #" + id);
        } catch (BusinessException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }
}

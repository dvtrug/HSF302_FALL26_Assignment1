package com.fu.cinema.controller.admin;

import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private static final String FORM_VIEW = "admin/customers/form";

    private final CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) String kw,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("customers", customerService.search(kw, page, PageUtils.DEFAULT_SIZE));
        model.addAttribute("kw", kw);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());
        return "admin/customers/list";
    }

}

package com.fu.cinema.controller;

import com.fu.cinema.dto.LoginForm;
import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(Model model,
                            HttpSession session,
                            @RequestParam(value = "redirect", required = false) String redirect) {
        if (SessionUtils.isLoggedIn(session)) {
            SessionUser user = SessionUtils.getCurrentUser(session);
            return user.isAdmin() ? "redirect:/admin" : "redirect:/";
        }
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult bindingResult,
                        @RequestParam(value = "redirect", required = false) String redirect,
                        HttpSession session,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("redirect", redirect);
            return "auth/login";
        }

        try {
            SessionUser sessionUser = authService.login(form.getUsername(), form.getPassword());
            session.setAttribute(SessionUtils.CURRENT_USER, sessionUser);
            redirectAttributes.addFlashAttribute("success", "Đăng nhập thành công! Xin chào " + sessionUser.getDisplayName());

            if (redirect != null && !redirect.isBlank() && !redirect.startsWith("/login") && !redirect.startsWith("/logout")) {
                return "redirect:" + redirect;
            }

            return sessionUser.isAdmin() ? "redirect:/admin" : "redirect:/";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("redirect", redirect);
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpSession session) {
        if (SessionUtils.isLoggedIn(session)) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           HttpSession session,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            SessionUser sessionUser = authService.register(form);
            session.setAttribute(SessionUtils.CURRENT_USER, sessionUser);
            redirectAttributes.addFlashAttribute("success", "Đăng ký tài khoản thành công! Chào mừng " + sessionUser.getDisplayName() + " đến với FU Cinema.");
            return "redirect:/";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("success", "Đã đăng xuất thành công!");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logoutGet(HttpSession session, RedirectAttributes redirectAttributes) {
        return logout(session, redirectAttributes);
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}

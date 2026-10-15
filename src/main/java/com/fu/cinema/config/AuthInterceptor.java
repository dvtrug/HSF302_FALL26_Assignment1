package com.fu.cinema.config;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);
        SessionUser user = session != null ? SessionUtils.getCurrentUser(session) : null;

        // Bảo vệ route dành cho Quản trị viên (/admin/**)
        if (uri.startsWith("/admin")) {
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login?redirect=" + uri);
                return false;
            }
            if (!user.isAdmin()) {
                response.sendRedirect(request.getContextPath() + "/error/403");
                return false;
            }
        }

        // Bảo vệ route dành cho Khách hàng (/customer/**)
        if (uri.startsWith("/customer")) {
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login?redirect=" + uri);
                return false;
            }
            if (!user.isCustomer()) {
                response.sendRedirect(request.getContextPath() + "/error/403");
                return false;
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null && !modelAndView.getViewName().startsWith("redirect:")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                SessionUser user = SessionUtils.getCurrentUser(session);
                if (user != null) {
                    modelAndView.addObject("currentUser", user);
                }
            }
        }
    }
}

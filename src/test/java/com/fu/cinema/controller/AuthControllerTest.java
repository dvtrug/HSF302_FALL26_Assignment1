package com.fu.cinema.controller;

import com.fu.cinema.config.AuthInterceptor;
import com.fu.cinema.controller.admin.AdminDashboardController;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService), new AdminDashboardController())
                .addInterceptors(new AuthInterceptor())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("GET /login: Hiển thị view login khi chưa đăng nhập")
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginForm"));
    }

    @Test
    @DisplayName("GET /admin: Chưa đăng nhập bị redirect về /login (TODO 15)")
    void testAdminAccess_Unauthenticated_RedirectsLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/admin"));
    }

    @Test
    @DisplayName("GET /admin: Customer truy cập bị chặn redirect về /error/403 (TODO 15)")
    void testAdminAccess_Customer_Forbidden() throws Exception {
        MockHttpSession session = new MockHttpSession();
        SessionUser customerUser = SessionUser.builder()
                .accountId(2L)
                .customerId(1L)
                .username("customer1")
                .role(Role.CUSTOMER)
                .build();
        session.setAttribute(SessionUtils.CURRENT_USER, customerUser);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/error/403"));
    }

    @Test
    @DisplayName("GET /admin: Admin truy cập thành công")
    void testAdminAccess_Admin_Allowed() throws Exception {
        MockHttpSession session = new MockHttpSession();
        SessionUser adminUser = SessionUser.builder()
                .accountId(1L)
                .username("admin")
                .role(Role.ADMIN)
                .build();
        session.setAttribute(SessionUtils.CURRENT_USER, adminUser);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @DisplayName("POST /login: Đăng nhập Admin thành công điều hướng về /admin (TODO 13)")
    void testLogin_Admin_Success() throws Exception {
        SessionUser adminUser = SessionUser.builder()
                .accountId(1L)
                .username("admin")
                .displayName("Administrator")
                .role(Role.ADMIN)
                .build();
        when(authService.login("admin", "Admin@123")).thenReturn(adminUser);

        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "Admin@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("POST /login: Đăng nhập Customer thành công điều hướng về / (TODO 13)")
    void testLogin_Customer_Success() throws Exception {
        SessionUser customerUser = SessionUser.builder()
                .accountId(2L)
                .customerId(10L)
                .username("customer1")
                .displayName("Nguyen Van An")
                .role(Role.CUSTOMER)
                .build();
        when(authService.login("customer1", "123456")).thenReturn(customerUser);

        mockMvc.perform(post("/login")
                        .param("username", "customer1")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("POST /login: Đăng nhập tài khoản LOCKED bị báo lỗi (BR-03)")
    void testLogin_LockedUser() throws Exception {
        when(authService.login("locked_user", "123456"))
                .thenThrow(new BusinessException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên."));

        mockMvc.perform(post("/login")
                        .param("username", "locked_user")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /logout: Xóa session và chuyển hướng về /login (TODO 13)")
    void testLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, SessionUser.builder().username("admin").build());

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
    }
}

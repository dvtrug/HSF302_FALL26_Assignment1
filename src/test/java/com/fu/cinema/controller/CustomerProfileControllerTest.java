package com.fu.cinema.controller;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerProfileControllerTest {
    @Autowired private WebApplicationContext context;
    @Autowired private AuthService auth;
    @Autowired private CustomerService customers;
    private MockMvc mvc;
    private MockHttpSession session;
    private SessionUser user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        user = auth.login("an.nv@gmail.com", "Customer@123");
        session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, user);
    }

    private MockHttpServletRequestBuilder updateProfile() {
        return post("/customer/profile").session(session)
                .param("fullName", "Nguyen Van An").param("email", "an.nv@gmail.com")
                .param("mobile", "0901234567").param("birthday", "2000-05-12")
                .param("identityCard", "079200001234");
    }

    @Test
    void rendersOwnProfileWithDisabledUsername() throws Exception {
        mvc.perform(get("/customer/profile").session(session))
                .andExpect(status().isOk()).andExpect(view().name("customer/profile"))
                .andExpect(content().string(containsString("Nguyen Van An")))
                .andExpect(content().string(containsString("disabled")));
    }

    @Test
    void updatesContactDetailsAndSessionWithoutChangingLoginIdentity() throws Exception {
        mvc.perform(updateProfile().with(request -> {
                    request.setParameter("fullName", " Updated Customer ");
                    request.setParameter("email", "updated@example.com");
                    request.setParameter("username", "forged-login");
                    return request;
                }))
                .andExpect(redirectedUrl("/customer/profile"))
                .andExpect(flash().attribute("success", "Đã cập nhật hồ sơ"));
        var updated = customers.getProfile(user.getCustomerId());
        assertThat(updated.getFullName()).isEqualTo("Updated Customer");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getUsername()).isEqualTo("an.nv@gmail.com");
        var current = SessionUtils.getCurrentUser(session);
        assertThat(current.getDisplayName()).isEqualTo("Updated Customer");
        assertThat(current.getUsername()).isEqualTo(user.getUsername());
        assertThat(current.getAccountId()).isEqualTo(user.getAccountId());
        assertThat(current.getCustomerId()).isEqualTo(user.getCustomerId());
        assertThat(current.getRole()).isEqualTo(Role.CUSTOMER);
        mvc.perform(get("/customer/profile").session(session))
                .andExpect(content().string(containsString("Xin chào, Updated Customer")));
        assertThat(auth.login("an.nv@gmail.com", "Customer@123").getDisplayName()).isEqualTo("Updated Customer");
    }

    @Test
    void rejectsDuplicatePhoneAndKeepsUsernameAndFormValues() throws Exception {
        mvc.perform(updateProfile().with(request -> {
                    request.setParameter("mobile", "0912345678");
                    return request;
                }))
                .andExpect(status().isOk()).andExpect(view().name("customer/profile"))
                .andExpect(model().attribute("error", "Số điện thoại đã được sử dụng"))
                .andExpect(content().string(containsString("an.nv@gmail.com")));
        assertThat(customers.getProfile(user.getCustomerId()).getMobile()).isEqualTo("0901234567");
    }

    @Test
    void validatesFieldsAndAgeWithoutSavingInvalidProfile() throws Exception {
        mvc.perform(post("/customer/profile").session(session).param("username", "forged-login"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form",
                        "fullName", "email", "mobile", "birthday", "identityCard"))
                .andExpect(content().string(containsString("an.nv@gmail.com")));
        mvc.perform(updateProfile().with(request -> {
                    request.setParameter("birthday", java.time.LocalDate.now().minusYears(12).toString());
                    return request;
                }))
                .andExpect(status().isOk()).andExpect(model().attribute("error", "Khách hàng phải từ đủ 13 tuổi"));
        assertThat(customers.getProfile(user.getCustomerId()).getFullName()).isEqualTo("Nguyen Van An");
    }

    @Test
    void preventsGuestsAndAdminsFromUsingCustomerProfile() throws Exception {
        mvc.perform(get("/customer/profile"))
                .andExpect(redirectedUrl("/login?redirect=/customer/profile"));
        var admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER,
                SessionUser.builder().accountId(1L).role(Role.ADMIN).build());
        mvc.perform(get("/customer/profile").session(admin)).andExpect(redirectedUrl("/error/403"));
    }
    private MockHttpServletRequestBuilder passwordRequest(String current, String next, String confirm) {
        return post("/customer/password").session(session)
                .param("currentPassword", current).param("newPassword", next).param("confirmPassword", confirm);
    }

    @Test
    void rendersPasswordFormAndProtectsPasswordRoutes() throws Exception {
        mvc.perform(get("/customer/password").session(session))
                .andExpect(status().isOk()).andExpect(view().name("customer/password"))
                .andExpect(model().attributeExists("form"));
        mvc.perform(get("/customer/password"))
                .andExpect(redirectedUrl("/login?redirect=/customer/password"));
        mvc.perform(post("/customer/password"))
                .andExpect(redirectedUrl("/login?redirect=/customer/password"));
    }

    @Test
    void changesOnlyCurrentAccountsPasswordAndAllowsLoginWithNewPassword() throws Exception {
        var accounts = context.getBean(com.fu.cinema.repository.AccountRepository.class);
        var encoder = context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
        var other = auth.login("binh.tt@gmail.com", "Customer@123");
        String otherHash = accounts.findById(other.getAccountId()).orElseThrow().getPassword();
        mvc.perform(passwordRequest("Customer@123", "NewPassword123", "NewPassword123")
                        .param("accountId", other.getAccountId().toString()))
                .andExpect(redirectedUrl("/customer/profile"))
                .andExpect(flash().attribute("success", "Đã đổi mật khẩu"));
        String hash = accounts.findById(user.getAccountId()).orElseThrow().getPassword();
        assertThat(hash).isNotEqualTo("NewPassword123");
        assertThat(encoder.matches("NewPassword123", hash)).isTrue();
        assertThat(encoder.matches("Customer@123", hash)).isFalse();
        assertThat(auth.login(user.getUsername(), "NewPassword123")).isNotNull();
        assertThat(accounts.findById(other.getAccountId()).orElseThrow().getPassword()).isEqualTo(otherHash);
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutChangingStoredHash() throws Exception {
        var accounts = context.getBean(com.fu.cinema.repository.AccountRepository.class);
        String before = accounts.findById(user.getAccountId()).orElseThrow().getPassword();
        mvc.perform(passwordRequest("WrongPassword123", "NewPassword123", "NewPassword123"))
                .andExpect(status().isOk()).andExpect(view().name("customer/password"))
                .andExpect(model().attribute("error", "Mật khẩu hiện tại không đúng"));
        assertThat(accounts.findById(user.getAccountId()).orElseThrow().getPassword()).isEqualTo(before);
    }

    @Test
    void rejectsMismatchingConfirmation() throws Exception {
        mvc.perform(passwordRequest("Customer@123", "NewPassword123", "DifferentPassword123"))
                .andExpect(status().isOk()).andExpect(view().name("customer/password"))
                .andExpect(model().attribute("error", "Mật khẩu nhập lại không khớp"));
        assertThat(auth.login(user.getUsername(), "Customer@123")).isNotNull();
    }

    @Test
    void rejectsMissingFieldsAndWeakNewPassword() throws Exception {
        mvc.perform(passwordRequest("", "", ""))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form",
                        "currentPassword", "newPassword", "confirmPassword"));
        mvc.perform(passwordRequest("Customer@123", "weak", "weak"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "newPassword"));
        assertThat(auth.login(user.getUsername(), "Customer@123")).isNotNull();
    }
}

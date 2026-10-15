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
}

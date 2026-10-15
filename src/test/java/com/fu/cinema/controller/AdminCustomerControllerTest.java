package com.fu.cinema.controller;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AdminCustomerControllerTest {
    @Autowired private WebApplicationContext context;
    private MockMvc mvc;
    private MockHttpSession admin;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER,
                SessionUser.builder().accountId(1L).username("admin").role(Role.ADMIN).build());
    }

    @Test
    void rendersCustomerSearchWithAccountStatus() throws Exception {
        mvc.perform(get("/admin/customers").queryParam("kw", "nguyen").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("admin/customers/list"))
                .andExpect(content().string(containsString("Nguyen Van An")))
                .andExpect(content().string(containsString("ACTIVE")))
                .andExpect(model().attribute("kw", "nguyen"));
    }

    @Test
    void rendersEmptySearchAndKeepsKeywordInPaginationBaseUrl() throws Exception {
        mvc.perform(get("/admin/customers").queryParam("kw", "unknown").queryParam("page", "1").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có dữ liệu")))
                .andExpect(model().attribute("pageUrl", "http://localhost/admin/customers?kw=unknown"));
    }

    @Test
    void protectsAdminCustomerRoutes() throws Exception {
        mvc.perform(get("/admin/customers"))
                .andExpect(redirectedUrl("/login?redirect=/admin/customers"));
        MockHttpSession customer = new MockHttpSession();
        customer.setAttribute(SessionUtils.CURRENT_USER,
                SessionUser.builder().accountId(2L).role(Role.CUSTOMER).build());
        mvc.perform(get("/admin/customers").session(customer))
                .andExpect(redirectedUrl("/error/403"));
    }
    @Test
    void displaysCreateFormAndValidationErrors() throws Exception {
        mvc.perform(get("/admin/customers/new").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("admin/customers/form"))
                .andExpect(model().attributeExists("form", "statuses"));
        mvc.perform(post("/admin/customers/new").session(admin))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form",
                        "fullName", "email", "mobile", "birthday", "identityCard"));
    }

    @Test
    void displaysEditFormAndPreservesPasswordWhenBlank() throws Exception {
        mvc.perform(get("/admin/customers/1/edit").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("admin/customers/form"));
        mvc.perform(post("/admin/customers/1/edit").session(admin)
                        .param("fullName", "Nguyen Van An").param("email", "an.nv@gmail.com")
                        .param("mobile", "0901234567").param("birthday", "2000-05-12")
                        .param("identityCard", "079200001234").param("password", "").param("status", "ACTIVE"))
                .andExpect(redirectedUrl("/admin/customers"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void rejectsDuplicateMobileAndRetainsFormValues() throws Exception {
        mvc.perform(post("/admin/customers/new").session(admin)
                        .param("fullName", "Duplicate Customer").param("email", "duplicate@example.com")
                        .param("mobile", "0901234567").param("birthday", "2000-05-12")
                        .param("identityCard", "123456789012").param("password", "Customer@123")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(view().name("admin/customers/form"))
                .andExpect(model().attribute("error", "Số điện thoại đã được sử dụng"))
                .andExpect(content().string(containsString("Duplicate Customer")));
    }
    @Test
    @org.springframework.transaction.annotation.Transactional
    void locksAndUnlocksThroughPostAndControlsLogin() throws Exception {
        mvc.perform(post("/admin/customers/1/lock").session(admin))
                .andExpect(redirectedUrl("/admin/customers"))
                .andExpect(flash().attributeExists("success"));
        var auth = context.getBean(com.fu.cinema.service.AuthService.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> auth.login("an.nv@gmail.com", "Customer@123"))
                .isInstanceOf(com.fu.cinema.exception.BusinessException.class);
        mvc.perform(post("/admin/customers/1/lock").session(admin))
                .andExpect(redirectedUrl("/admin/customers"));
        org.assertj.core.api.Assertions.assertThat(auth.login("an.nv@gmail.com", "Customer@123")).isNotNull();
    }
    @Test
    @org.springframework.transaction.annotation.Transactional
    void deleteLocksCustomerWithBookingsAndDeletesNewCustomer() throws Exception {
        mvc.perform(post("/admin/customers/1/delete").session(admin))
                .andExpect(redirectedUrl("/admin/customers"))
                .andExpect(flash().attribute("success", "Khách hàng đã có đơn đặt vé nên chỉ khoá tài khoản"));
        var customers = context.getBean(com.fu.cinema.repository.CustomerRepository.class);
        var accounts = context.getBean(com.fu.cinema.repository.AccountRepository.class);
        org.assertj.core.api.Assertions.assertThat(customers.findById(1L).orElseThrow().getAccount().getStatus())
                .isEqualTo(com.fu.cinema.enums.AccountStatus.LOCKED);
        mvc.perform(post("/admin/customers/new").session(admin)
                        .param("fullName", "Delete Test").param("email", "delete.test@example.com")
                        .param("mobile", "0987654321").param("birthday", "2000-05-12")
                        .param("identityCard", "123456789012").param("password", "Customer@123")
                        .param("status", "ACTIVE"))
                .andExpect(redirectedUrl("/admin/customers"));
        var account = accounts.findByUsername("delete.test@example.com").orElseThrow();
        var customer = customers.findByAccount_AccountId(account.getAccountId()).orElseThrow();
        mvc.perform(post("/admin/customers/{id}/delete", customer.getCustomerId()).session(admin))
                .andExpect(redirectedUrl("/admin/customers"))
                .andExpect(flash().attribute("success", "Đã xoá khách hàng"));
        customers.flush();
        org.assertj.core.api.Assertions.assertThat(customers.existsById(customer.getCustomerId())).isFalse();
        org.assertj.core.api.Assertions.assertThat(accounts.existsById(account.getAccountId())).isFalse();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void renderedPaginationKeepsSearchKeyword() throws Exception {
        var service = context.getBean(com.fu.cinema.service.CustomerService.class);
        for (int i = 0; i < 11; i++) {
            var f = new com.fu.cinema.dto.CustomerForm();
            f.setFullName("Paging Test " + i);
            f.setEmail("paging" + i + "@gmail.com");
            f.setMobile(String.format("09900000%02d", i));
            f.setIdentityCard(String.format("9990000000%02d", i));
            f.setBirthday(java.time.LocalDate.of(2000, 1, 1));
            f.setPassword("Customer@123");
            service.create(f);
        }
        mvc.perform(get("/admin/customers").queryParam("kw", "GMAIL").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("kw=GMAIL&amp;page=1")));
    }
}

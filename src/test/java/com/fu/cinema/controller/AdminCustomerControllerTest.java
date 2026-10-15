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
}
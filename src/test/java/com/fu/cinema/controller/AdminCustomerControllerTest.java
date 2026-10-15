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
}

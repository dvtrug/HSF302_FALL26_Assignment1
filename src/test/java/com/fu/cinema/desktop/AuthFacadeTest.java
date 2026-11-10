package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuthFacadeTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private DesktopSession session;

    @Test
    void adminAccount_canLoginDesktop() {
        SessionUser user = authFacade.loginAdmin("admin@fucinema.vn", "Admin@123");
        assertNotNull(user);
        assertTrue(user.isAdmin());

        session.login(user);
        session.requireAdmin();
    }

    @Test
    void customerAccount_cannotLoginDesktop_throwsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                authFacade.loginAdmin("an.nv@gmail.com", "Customer@123"));
        assertTrue(ex.getMessage().contains("chỉ dành cho nhân viên"));
    }

    @Test
    void customerAccount_canLoginKioskMode() {
        SessionUser user = authFacade.loginKiosk("an.nv@gmail.com", "Customer@123");
        assertNotNull(user);
        assertTrue(user.isCustomer());
    }

    @Test
    void wrongPassword_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                authFacade.loginAdmin("admin@fucinema.vn", "WrongPassword"));
    }

    @Test
    void requireAdmin_failsWhenNotLoggedIn() {
        session.logout();
        assertThrows(BusinessException.class, () -> session.requireAdmin());
    }
}

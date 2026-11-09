package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:desktopfacade;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class DesktopFacadeTest {

    @Autowired private AuthFacade auth;
    @Autowired private DesktopSession session;

    @BeforeEach
    @AfterEach
    void clearSession() {
        session.logout();
    }

    @Test
    void customerCredentialsDoNotOpenAnAdminSession() {
        var error = assertThrows(BusinessException.class,
                () -> session.login(auth.loginAdmin("an.nv@gmail.com", "Customer@123")));

        assertTrue(error.getMessage().contains("ADMIN"));
        assertNull(session.getUser());
        assertEquals("", session.displayNameProperty().get());
    }
}

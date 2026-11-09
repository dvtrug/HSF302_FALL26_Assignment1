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

    @Test
    void adminSessionPublishesDisplayNameAndClearsItAtLogout() {
        var names = new java.util.ArrayList<String>();
        javafx.beans.value.ChangeListener<String> listener = (observable, previous, name) -> names.add(name);
        session.displayNameProperty().addListener(listener);
        try {
            var admin = auth.loginAdmin("admin", "Admin@123");
            assertFalse(admin.getDisplayName().isBlank());
            session.login(admin);

            assertSame(admin, session.getUser());
            assertDoesNotThrow(session::requireAdmin);
            session.logout();

            assertNull(session.getUser());
            assertEquals(java.util.List.of(admin.getDisplayName(), ""), names);
        } finally {
            session.displayNameProperty().removeListener(listener);
        }
    }

    @Test
    void lockedAccountRemainsSignedOut() {
        var error = assertThrows(BusinessException.class,
                () -> session.login(auth.loginAdmin("locked_user", "123456")));

        assertTrue(error.getMessage().contains("bị khóa"));
        assertNull(session.getUser());
        assertThrows(BusinessException.class, session::requireAdmin);
    }
}

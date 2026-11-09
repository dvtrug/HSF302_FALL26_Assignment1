package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.DashboardData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:dashboardfacade;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class DashboardFacadeTest {

    @Autowired
    private DashboardFacade dashboardFacade;

    @Test
    void load_returnsValidMetricsAndRoomCounts() {
        DashboardData data = dashboardFacade.load();

        assertNotNull(data, "DashboardData should not be null");
        assertTrue(data.showingMovies() >= 0, "Showing movies count should be non-negative");
        assertTrue(data.customers() >= 0, "Customers count should be non-negative");
        assertTrue(data.pendingBookings() >= 0, "Pending bookings count should be non-negative");
        assertNotNull(data.todayByRoom(), "Room counts list should not be null");
    }
}

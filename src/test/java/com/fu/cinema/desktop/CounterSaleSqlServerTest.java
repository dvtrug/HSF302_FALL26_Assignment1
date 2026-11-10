package com.fu.cinema.desktop;

import com.fu.cinema.desktop.component.SeatMapView;
import com.fu.cinema.desktop.facade.CounterSaleFacade;
import com.fu.cinema.desktop.model.CounterMovieItem;
import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.util.SessionUtils;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Opt-in only: create-drop is restricted to a dedicated local QA database. */
@EnabledIfEnvironmentVariable(named = "FU_COUNTER_QA_URL",
        matches = "jdbc:sqlserver://localhost:1433;databaseName=FU_Cinema_QA_B55_[0-9]+;.*")
@SpringBootTest(properties = "spring.main.web-application-type=servlet")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CounterSaleSqlServerTest extends CounterSaleControllerTest {
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean CounterSaleFacade sales;

    @DynamicPropertySource
    static void sqlServer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("FU_COUNTER_QA_URL"));
        registry.add("spring.datasource.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        registry.add("spring.datasource.username", () -> System.getenv("FU_COUNTER_QA_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("FU_COUNTER_QA_PASSWORD"));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    @SuppressWarnings("unchecked")
    void webEndpointAndJavaFxCompeteForOneSeatOnSqlServer() throws Exception {
        session.login(SessionUser.builder().accountId(1L).role(Role.ADMIN).displayName("QA Admin").build());
        Stage stage = onFx(Stage::new);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var loader = onFx(() -> {
                var view = new FXMLLoader(getClass().getResource(View.COUNTER_SALE.path()));
                view.setControllerFactory(context::getBean);
                Parent root = view.load();
                var scene = new Scene(root, 1000, 650);
                scene.getStylesheets().add(getClass().getResource("/desktop/css/desktop.css").toExternalForm());
                stage.setScene(scene);
                stage.show();
                return view;
            });
            var movies = (ComboBox<CounterMovieItem>) loader.getNamespace().get("movie");
            var times = (ComboBox<ShowtimeItem>) loader.getNamespace().get("showtime");
            var customers = (ListView<CustomerItem>) loader.getNamespace().get("customerResults");
            var map = (SeatMapView) loader.getNamespace().get("seatMap");
            var sell = (Button) loader.getNamespace().get("sellButton");
            onFx(() -> { ((TextField) loader.getNamespace().get("customerQuery")).setText("Nguyen Van An"); return null; });
            awaitFx(() -> !movies.getItems().isEmpty() && !customers.getItems().isEmpty());
            onFx(() -> {
                customers.getSelectionModel().selectFirst();
                movies.setValue(movies.getItems().stream().filter(item -> item.title().equals("Biệt Đội Siêu Tốc"))
                        .findFirst().orElseThrow());
                return null;
            });
            awaitFx(() -> !times.getItems().isEmpty());
            onFx(() -> { times.getSelectionModel().selectFirst(); return null; });
            awaitFx(map::isVisible);
            long showtimeId = onFx(() -> times.getValue().id());
            long customerId = onFx(() -> customers.getSelectionModel().getSelectedItem().id());
            doAnswer(invocation -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return invocation.callRealMethod();
            }).when(sales).sell(eq(customerId), eq(showtimeId), anyList(), anyBoolean());
            var webSession = new MockHttpSession();
            webSession.setAttribute(SessionUtils.CURRENT_USER, SessionUser.builder().customerId(customerId)
                    .role(Role.CUSTOMER).displayName("QA customer").build());
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
            var web = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return mvc.perform(post("/customer/bookings").session(webSession)
                        .param("showtimeId", Long.toString(showtimeId)).param("seatCodes", "C8")).andReturn();
            });
            onFx(() -> { seat(map, "C8").fire(); return null; });
            Platform.runLater(sell::fire);
            answerDialog(stage, "C8", ButtonType.YES);
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both requests must reach the start barrier");
            start.countDown();
            var response = web.get(20, TimeUnit.SECONDS);
            assertEquals(302, response.getResponse().getStatus());
            boolean webWon = response.getFlashMap().containsKey("success");
            if (!webWon) assertTrue(response.getFlashMap().get("error").toString().contains("Ghế đã có người đặt"));
            answerDialog(stage, webWon ? "Ghế đã có người đặt" : "Đã tạo đơn", ButtonType.OK,
                    "08-sqlserver-race-dialog.png", "C8");
            awaitFx(() -> map.isVisible() && seat(map, "C8").isDisabled());
            int active = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM tickets t JOIN bookings b ON t.booking_id=b.booking_id
                    WHERE t.showtime_id=? AND t.seat_code='C8' AND b.status<>'CANCELLED'
                    """, Integer.class, showtimeId);
            var duplicates = jdbc.queryForList("""
                    SELECT t.showtime_id,t.seat_code,COUNT(*) AS active_count
                    FROM tickets t JOIN bookings b ON t.booking_id=b.booking_id
                    WHERE b.status<>'CANCELLED' GROUP BY t.showtime_id,t.seat_code HAVING COUNT(*)>1
                    """);
            assertEquals(1, active);
            assertTrue(duplicates.isEmpty(), duplicates.toString());
            onFx(() -> { capture(stage.getScene().getRoot(), "09-sqlserver-sold-seat.png"); return null; });
            System.out.println("TC-D16 SQL Server: webWon=" + webWon + ", desktopWon=" + !webWon
                    + ", activeTickets=" + active + ", duplicateSeatGroups=" + duplicates.size());
        } finally {
            start.countDown();
            session.logout();
            onFx(() -> {
                Window.getWindows().stream().filter(window -> window != stage).toList().forEach(Window::hide);
                stage.close();
                return null;
            });
        }
    }
}

package com.fu.cinema.desktop;

import com.fu.cinema.desktop.component.SeatMapView;
import com.fu.cinema.desktop.model.CounterMovieItem;
import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.service.BookingService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:counterui;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class CounterSaleControllerTest {

    @Autowired ApplicationContext context;
    @Autowired DesktopSession session;
    @Autowired BookingService bookings;

    @BeforeAll
    static void startJavaFx() {
        assumeTrue(!System.getProperty("os.name").startsWith("Linux")
                || System.getenv("DISPLAY") != null, "JavaFX tests require an X display");
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            // Another test has already started the toolkit.
        }
    }

    @Test
    void sellsPaidSeatsAndRefreshesWhenAnotherBuyerGetsTheNextSeat() throws Exception {
        session.login(SessionUser.builder().accountId(1L).role(Role.ADMIN).displayName("Admin").build());
        var stage = onFx(Stage::new);
        try {
            var view = onFx(() -> {
                var loader = new FXMLLoader(getClass().getResource(View.COUNTER_SALE.path()));
                loader.setControllerFactory(context::getBean);
                Parent root = loader.load();
                var scene = new Scene(root, 1000, 650);
                scene.getStylesheets().add(getClass().getResource("/desktop/css/desktop.css").toExternalForm());
                stage.setScene(scene);
                stage.show();
                return loader;
            });
            verifySaleAndConflict(view, stage);
        } finally {
            session.logout();
            onFx(() -> {
                Window.getWindows().stream().filter(window -> window != stage).toList().forEach(Window::hide);
                stage.close();
                return null;
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void verifySaleAndConflict(FXMLLoader view, Stage stage) throws Exception {
        var movies = (ComboBox<CounterMovieItem>) view.getNamespace().get("movie");
        var showtimes = (ComboBox<ShowtimeItem>) view.getNamespace().get("showtime");
        var customers = (ListView<CustomerItem>) view.getNamespace().get("customerResults");
        var map = (SeatMapView) view.getNamespace().get("seatMap");
        var sell = (Button) view.getNamespace().get("sellButton");

        onFx(() -> {
            ((TextField) view.getNamespace().get("customerQuery")).setText("Nguyen Van An");
            return null;
        });
        awaitFx(() -> !movies.getItems().isEmpty() && !customers.getItems().isEmpty());
        onFx(() -> {
            movies.setValue(movies.getItems().stream().filter(movie -> movie.title().equals("Biệt Đội Siêu Tốc"))
                    .findFirst().orElseThrow());
            customers.getSelectionModel().selectFirst();
            return null;
        });
        awaitFx(() -> !showtimes.getItems().isEmpty());
        onFx(() -> {
            showtimes.getSelectionModel().selectFirst();
            return null;
        });
        awaitFx(map::isVisible);

        onFx(() -> {
            seat(map, "A1").fire();
            var total = (Label) view.getNamespace().get("total");
            assertTrue(total.getText().contains("1 ghế"));
            assertTrue(total.getText().contains("80.000"));
            ((CheckBox) view.getNamespace().get("paid")).setSelected(true);
            assertFalse(sell.isDisabled());
            sell.fire();
            return null;
        });
        closeDialog(stage, "CONFIRMED");
        awaitFx(() -> map.isVisible() && seat(map, "A1").isDisabled());

        var request = new BookingRequest();
        request.setShowtimeId(onFx(() -> showtimes.getValue().id()));
        request.setSeatCodes(List.of("A2"));
        Long customerId = onFx(() -> customers.getSelectionModel().getSelectedItem().id());
        assertEquals(BookingStatus.PENDING, bookings.create(customerId, request).getStatus());

        onFx(() -> {
            seat(map, "A2").fire();
            assertFalse(sell.isDisabled());
            sell.fire();
            return null;
        });
        closeDialog(stage, "Ghế đã có người đặt");
        awaitFx(() -> map.isVisible() && seat(map, "A2").isDisabled());
        assertTrue(onFx(sell::isDisabled));
    }

    private static ToggleButton seat(SeatMapView view, String code) {
        return view.lookupAll(".seat").stream().map(ToggleButton.class::cast)
                .filter(button -> code.equals(button.getText())).findFirst().orElseThrow();
    }

    private static void closeDialog(Stage stage, String expected) throws Exception {
        awaitFx(() -> Window.getWindows().stream().anyMatch(window ->
                window != stage && window.isShowing() && window.getScene().getRoot() instanceof DialogPane));
        onFx(() -> {
            var window = Window.getWindows().stream().filter(other ->
                    other != stage && other.isShowing() && other.getScene().getRoot() instanceof DialogPane)
                    .findFirst().orElseThrow();
            var pane = (DialogPane) window.getScene().getRoot();
            try {
                assertTrue(pane.getContentText().contains(expected), pane.getContentText());
            } finally {
                window.hide();
            }
            return null;
        });
    }

    private static void awaitFx(Callable<Boolean> predicate) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        do {
            if (onFx(predicate)) return;
            Thread.sleep(60);
        } while (System.nanoTime() < end);
        fail("Timed out waiting for JavaFX state");
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}

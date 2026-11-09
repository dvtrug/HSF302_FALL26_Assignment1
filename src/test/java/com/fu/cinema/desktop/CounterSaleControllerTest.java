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
import com.fu.cinema.repository.BookingRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
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
    @Autowired BookingRepository bookingRepository;

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
            seat(map, "A8").fire();
            movies.setValue(null);
            assertNull(showtimes.getValue());
            assertTrue(showtimes.getItems().isEmpty());
            assertTrue(map.getSelectedSeats().isEmpty(), "Changing movie must clear the previous seat selection");
            assertTrue(map.lookupAll(".seat").isEmpty(), "Old room seats must be discarded immediately");
            assertTrue(sell.isDisabled());
            assertTrue(((Label) view.getNamespace().get("total")).getText().startsWith("0 ghế"));
            movies.setValue(movies.getItems().stream().filter(item -> item.title().equals("Biệt Đội Siêu Tốc"))
                    .findFirst().orElseThrow());
            return null;
        });
        awaitFx(() -> !showtimes.getItems().isEmpty());
        onFx(() -> { showtimes.getSelectionModel().selectFirst(); return null; });
        awaitFx(map::isVisible);

        onFx(() -> {
            seat(map, "A1").fire();
            seat(map, "A3").fire();
            seat(map, "A4").fire();
            var total = (Label) view.getNamespace().get("total");
            assertTrue(total.getText().contains("3 ghế"));
            assertTrue(total.getText().contains("240.000"));
            ((CheckBox) view.getNamespace().get("paid")).setSelected(true);
            assertFalse(sell.isDisabled());
            capture(stage.getScene().getRoot(), "02-three-seats-total.png");
            return null;
        });
        long before = bookingRepository.count();
        Platform.runLater(sell::fire);
        answerDialog(stage, "A1", ButtonType.NO);
        assertEquals(before, bookingRepository.count(), "Declining must not write a booking");
        assertFalse(onFx(sell::isDisabled));
        Platform.runLater(sell::fire);
        answerDialog(stage, "240.000", ButtonType.YES);
        answerDialog(stage, "CONFIRMED", ButtonType.OK, "03-paid-confirmed.png", "A1, A3, A4", "240.000", "#");
        awaitFx(() -> map.isVisible() && seat(map, "A1").isDisabled());

        var request = new BookingRequest();
        request.setShowtimeId(onFx(() -> showtimes.getValue().id()));
        request.setSeatCodes(List.of("A2"));
        Long customerId = onFx(() -> customers.getSelectionModel().getSelectedItem().id());
        assertEquals(BookingStatus.PENDING, bookings.create(customerId, request).getStatus());

        onFx(() -> {
            seat(map, "A2").fire();
            assertFalse(sell.isDisabled());
            return null;
        });
        Platform.runLater(sell::fire);
        answerDialog(stage, "A2", ButtonType.YES);
        answerDialog(stage, "Ghế đã có người đặt", ButtonType.OK, "06-seat-conflict.png", "A2");
        awaitFx(() -> map.isVisible() && seat(map, "A2").isDisabled());
        assertTrue(onFx(sell::isDisabled));

        onFx(() -> {
            seat(map, "A5").fire();
            assertFalse(((CheckBox) view.getNamespace().get("paid")).isSelected());
            return null;
        });
        Platform.runLater(sell::fire);
        answerDialog(stage, "PENDING", ButtonType.YES);
        answerDialog(stage, "PENDING", ButtonType.OK, "04-unpaid-pending.png", "A5", "80.000", "#");
        awaitFx(() -> map.isVisible() && seat(map, "A5").isDisabled());

        onFx(() -> { ((TextField) view.getNamespace().get("customerQuery")).setText("Tran Thi Binh"); return null; });
        awaitFx(() -> !customers.getItems().isEmpty());
        onFx(() -> {
            customers.getSelectionModel().selectFirst();
            movies.setValue(movies.getItems().stream().filter(item -> item.title().equals("Ngôi Nhà Bí Ẩn"))
                    .findFirst().orElseThrow());
            return null;
        });
        awaitFx(() -> !showtimes.getItems().isEmpty());
        onFx(() -> { showtimes.getSelectionModel().selectFirst(); return null; });
        awaitFx(map::isVisible);
        long beforeUnderage = bookingRepository.count();
        onFx(() -> { seat(map, "A1").fire(); return null; });
        Platform.runLater(sell::fire);
        answerDialog(stage, "PENDING", ButtonType.YES);
        answerDialog(stage, "18 tuổi", ButtonType.OK, "05-underage-error.png", "Ngôi Nhà Bí Ẩn");
        awaitFx(() -> map.isVisible() && !seat(map, "A1").isDisabled());
        assertEquals(beforeUnderage, bookingRepository.count(), "Underage failure must not create an order");
        assertTrue(onFx(() -> map.getSelectedSeats().isEmpty() && sell.isDisabled()));
        onFx(() -> { capture(stage.getScene().getRoot(), "07-refreshed-seat-map.png"); return null; });
    }

    static ToggleButton seat(SeatMapView view, String code) {
        return view.lookupAll(".seat").stream().map(ToggleButton.class::cast)
                .filter(button -> code.equals(button.getText())).findFirst().orElseThrow();
    }

    private static void closeDialog(Stage stage, String expected) throws Exception {
        answerDialog(stage, expected, ButtonType.OK);
    }

    static void answerDialog(Stage stage, String expected, ButtonType answer, String... proofAndExpected) throws Exception {
        awaitFx(() -> Window.getWindows().stream().anyMatch(window ->
                window != stage && window.isShowing() && window.getScene().getRoot() instanceof DialogPane));
        onFx(() -> {
            var window = Window.getWindows().stream().filter(other ->
                    other != stage && other.isShowing() && other.getScene().getRoot() instanceof DialogPane)
                    .findFirst().orElseThrow();
            var pane = (DialogPane) window.getScene().getRoot();
            try {
                var content = ((javafx.scene.control.ScrollPane) pane.getContent()).getContent();
                String text = ((Label) content).getText();
                assertTrue(text.contains(expected), text);
                if (proofAndExpected.length > 0) {
                    for (int i = 1; i < proofAndExpected.length; i++) assertTrue(text.contains(proofAndExpected[i]), text);
                    capture(pane, proofAndExpected[0]);
                }
                assertNotNull(pane.lookupButton(answer), "Expected confirmation/notification button " + answer);
                ((Button) pane.lookupButton(answer)).fire();
            } finally {
                window.hide();
            }
            return null;
        });
    }

    static void capture(Node node, String filename) throws Exception {
        String directory = System.getProperty("fu.evidence.dir");
        if (directory == null) return;
        node.applyCss();
        if (node instanceof Parent parent) parent.layout();
        var snapshot = node.snapshot(null, null);
        var image = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var pixels = snapshot.getPixelReader();
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, pixels.getArgb(x, y));
        Path destination = Path.of(directory).resolve(filename);
        Files.createDirectories(destination.getParent());
        ImageIO.write(image, "png", destination.toFile());
    }

    static void awaitFx(Callable<Boolean> predicate) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        do {
            if (onFx(predicate)) return;
            Thread.sleep(60);
        } while (System.nanoTime() < end);
        fail("Timed out waiting for JavaFX state");
    }

    static <T> T onFx(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}

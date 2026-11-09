package com.fu.cinema.desktop.component;

import com.fu.cinema.desktop.model.SeatMapData;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SeatMapViewTest {

    @BeforeAll
    static void startJavaFx() {
        assumeTrue(!System.getProperty("os.name").startsWith("Linux")
                || System.getenv("DISPLAY") != null, "JavaFX tests require an X display");
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            // Other JavaFX tests may have started the toolkit.
        }
    }

    @Test
    void renders96SeatsAndDisablesSoldSeats() throws Exception {
        onFxThread(() -> {
            SeatMapView view = new SeatMapView();
            view.setData(new SeatMapData(List.of("A", "B", "C", "D", "E", "F", "G", "H"), 12, Set.of("A1")));
            List<ToggleButton> seats = buttons(view);

            assertEquals(96, seats.size());
            ToggleButton sold = seats.getFirst();
            assertTrue(sold.isDisabled());
            assertTrue(sold.getPseudoClassStates().contains(PseudoClass.getPseudoClass("sold")));
            sold.fire();
            assertTrue(view.getSelectedSeats().isEmpty());
        });
    }

    @Test
    void rejectsNinthSeatAndResetsSelectionOnReload() throws Exception {
        onFxThread(() -> {
            AtomicInteger warnings = new AtomicInteger();
            SeatMapView view = new SeatMapView(warnings::incrementAndGet);
            view.setData(new SeatMapData(List.of("A"), 9, Set.of()));
            List<ToggleButton> seats = buttons(view);
            var selection = view.getSelectedSeats();

            seats.forEach(ToggleButton::fire);
            assertEquals(8, selection.size());
            assertEquals(1, warnings.get());
            assertFalse(seats.get(8).isSelected());
            assertThrows(UnsupportedOperationException.class, () -> selection.add("A9"));

            seats.getFirst().fire();
            seats.get(8).fire();
            assertEquals(List.of("A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9"), selection);

            view.setData(new SeatMapData(List.of("B"), 4, Set.of("B1")));
            assertTrue(selection.isEmpty());
            assertEquals(4, buttons(view).size());
        });
    }

    private static List<ToggleButton> buttons(SeatMapView view) {
        GridPane grid = (GridPane) view.getChildren().get(1);
        return grid.getChildren().stream().filter(ToggleButton.class::isInstance)
                .map(ToggleButton.class::cast).toList();
    }

    private static void onFxThread(Runnable assertions) throws Exception {
        var task = new FutureTask<>(assertions, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}

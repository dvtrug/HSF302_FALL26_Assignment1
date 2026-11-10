package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.ReportController;
import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportControllerTest {

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
    void highlightsInvalidDatesWithoutCallingTheFacade() throws Exception {
        var facade = mock(ReportFacade.class);
        var async = new FxAsync();
        try {
            onFx(() -> {
                var view = load(facade, async);
                var from = (DatePicker) view.getNamespace().get("fromDate");
                var to = (DatePicker) view.getNamespace().get("toDate");
                var generate = button(view, "generateButton");
                var error = PseudoClass.getPseudoClass("error");

                from.setValue(null);
                generate.fire();
                assertTrue(from.getPseudoClassStates().contains(error));
                assertNotNull(from.getTooltip());

                from.setValue(LocalDate.of(2026, 11, 9));
                to.setValue(from.getValue().minusDays(1));
                generate.fire();
                assertFalse(from.getPseudoClassStates().contains(error));
                assertTrue(to.getPseudoClassStates().contains(error));
                assertTrue(to.getTooltip().getText().contains("sau hoặc bằng"));
                return null;
            });
            verifyNoInteractions(facade);
        } finally {
            async.destroy();
        }
    }

    @Test
    void showsProgressAndSortsAllBookingsAcrossPages() throws Exception {
        var facade = mock(ReportFacade.class);
        var async = new FxAsync();
        var release = new CountDownLatch(1);
        var completed = new CountDownLatch(1);
        var day = LocalDate.of(2026, 11, 9).atStartOfDay();
        List<ReportBookingRow> rows = IntStream.iterate(16, id -> id - 1).limit(16)
                .mapToObj(id -> new ReportBookingRow(id, day.plusMinutes(id), "Khách", "Phim", 1, BigDecimal.ONE))
                .toList();
        when(facade.generate(any(), any())).thenAnswer(call -> {
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Report task was not released");
            }
            return new ReportData(List.of(), rows, 16, 16, BigDecimal.valueOf(16));
        });

        try {
            var view = onFx(() -> {
                var loader = load(facade, async);
                var status = (Label) loader.getNamespace().get("pageStatus");
                status.textProperty().addListener((observable, before, after) -> {
                    if ("Trang 1 / 2".equals(after)) {
                        completed.countDown();
                    }
                });
                button(loader, "generateButton").fire();
                assertTrue(((ProgressIndicator) loader.getNamespace().get("progress")).isVisible());
                assertTrue(button(loader, "generateButton").isDisabled());
                return loader;
            });
            release.countDown();
            assertTrue(completed.await(10, TimeUnit.SECONDS));
            onFx(() -> {
                assertFalse(((ProgressIndicator) view.getNamespace().get("progress")).isVisible());
                @SuppressWarnings("unchecked") var table = (TableView<ReportBookingRow>) view.getNamespace().get("bookingsTable");
                assertEquals(15, table.getItems().size());
                assertEquals(16, table.getItems().getFirst().id());

                button(view, "nextPage").fire();
                assertEquals(List.of(rows.getLast()), table.getItems());
                assertTrue(button(view, "nextPage").isDisabled());

                var idColumn = table.getColumns().getFirst();
                idColumn.setSortType(TableColumn.SortType.ASCENDING);
                table.getSortOrder().add(idColumn);
                assertEquals(1, table.getItems().getFirst().id());
                button(view, "nextPage").fire();
                assertEquals(List.of(rows.getFirst()), table.getItems());
                return null;
            });
        } finally {
            release.countDown();
            async.destroy();
        }
    }

    private static FXMLLoader load(ReportFacade facade, FxAsync async) throws Exception {
        var loader = new FXMLLoader(ReportController.class.getResource("/desktop/fxml/report.fxml"));
        loader.setControllerFactory(type -> new ReportController(facade, async));
        loader.load();
        return loader;
    }

    private static Button button(FXMLLoader loader, String name) {
        return (Button) loader.getNamespace().get(name);
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}

package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@Component
@Scope("prototype")
public class ReportController {

    private static final DateTimeFormatter BOOKING_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private PieChart ticketsChart;
    @FXML private TableView<ReportBookingRow> bookingsTable;
    @FXML private TableColumn<ReportBookingRow, Long> idColumn;
    @FXML private TableColumn<ReportBookingRow, LocalDateTime> dateColumn;
    @FXML private TableColumn<ReportBookingRow, String> customerColumn;
    @FXML private TableColumn<ReportBookingRow, String> movieColumn;
    @FXML private TableColumn<ReportBookingRow, Integer> ticketColumn;
    @FXML private TableColumn<ReportBookingRow, BigDecimal> totalColumn;

    @FXML private BarChart<String, Number> revenueChart;

    @FXML
    private void initialize() {
        bindColumn(idColumn, ReportBookingRow::id);
        bindColumn(dateColumn, ReportBookingRow::bookingDate);
        bindColumn(customerColumn, ReportBookingRow::customerName);
        bindColumn(movieColumn, ReportBookingRow::movieTitle);
        bindColumn(ticketColumn, ReportBookingRow::ticketCount);
        bindColumn(totalColumn, ReportBookingRow::totalAmount);
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : BOOKING_DATE.format(date));
            }
        });
        totalColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : MoneyUtil.format(amount));
            }
        });
    }

    private static <T> void bindColumn(TableColumn<ReportBookingRow, T> column,
                                       Function<ReportBookingRow, T> value) {
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(value.apply(row.getValue())));
    }

    private void render(ReportData data) {
        renderRevenue(data.revenueByMovie());
        ticketsChart.getData().setAll(data.revenueByMovie().stream()
                .map(row -> new PieChart.Data(row.movieTitle() + " (" + row.ticketCount() + " vé)", row.ticketCount()))
                .toList());
        bookingsTable.getItems().setAll(data.bookings());
    }

    private void renderRevenue(List<RevenueRow> rows) {
        revenueChart.getData().clear();
        if (rows.isEmpty()) {
            return;
        }
        var series = new XYChart.Series<String, Number>();
        int index = 1;
        for (RevenueRow row : rows) {
            // Prefix the category so different movies with the same title have separate bars.
            var bar = new XYChart.Data<String, Number>(index++ + ". " + row.movieTitle(), row.revenue());
            bar.nodeProperty().addListener((observable, previous, node) -> {
                if (node != null) {
                    Tooltip.install(node, new Tooltip(row.movieTitle() + "\n" + row.ticketCount()
                            + " vé\n" + MoneyUtil.format(row.revenue())));
                }
            });
            series.getData().add(bar);
        }
        revenueChart.getData().add(series);
    }
}

package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.CsvExporter;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ReportController {

    private static final DateTimeFormatter BOOKING_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReportFacade reports;
    private final FxAsync async;

    @FXML private HBox filters;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Button exportButton;
    @FXML private Label status;
    @FXML private PieChart ticketsChart;
    @FXML private TableView<ReportBookingRow> bookingsTable;
    @FXML private TableColumn<ReportBookingRow, Long> idColumn;
    @FXML private TableColumn<ReportBookingRow, LocalDateTime> dateColumn;
    @FXML private TableColumn<ReportBookingRow, String> customerColumn;
    @FXML private TableColumn<ReportBookingRow, String> movieColumn;
    @FXML private TableColumn<ReportBookingRow, Integer> ticketColumn;
    @FXML private TableColumn<ReportBookingRow, BigDecimal> totalColumn;

    @FXML private BarChart<String, Number> revenueChart;

    private ReportData current;

    @FXML
    private void initialize() {
        LocalDate today = LocalDate.now();
        fromDate.setValue(today.minusDays(6));
        toDate.setValue(today);
        fromDate.valueProperty().addListener((observable, previous, value) -> datesChanged());
        toDate.valueProperty().addListener((observable, previous, value) -> datesChanged());

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

    @FXML
    private void onGenerate() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        clearReport("Đang tải báo cáo…");
        async.run(() -> reports.generate(from, to), this::render, error -> {
            status.setText("Không thể tạo báo cáo.");
            if (filters.getScene() != null) {
                AlertUtil.showError(error);
            }
        }, filters);
    }

    @FXML
    private void onExport() {
        if (current == null || current.isEmpty()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Xuất báo cáo doanh thu");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("doanh-thu-" + fromDate.getValue() + "_" + toDate.getValue() + ".csv");
        var file = chooser.showSaveDialog(filters.getScene().getWindow());
        if (file == null) {
            return;
        }
        var bookings = current.bookings();
        async.run(() -> {
            CsvExporter.writeBookings(file.toPath(), bookings);
            return null;
        }, ignored -> {
            if (filters.getScene() != null) {
                AlertUtil.info("Đã xuất báo cáo CSV.");
            }
        }, error -> {
            if (filters.getScene() != null) {
                AlertUtil.showError(error);
            }
        }, filters);
    }

    private void datesChanged() {
        clearReport("Khoảng ngày đã thay đổi. Chọn Xem báo cáo để cập nhật.");
    }

    private void clearReport(String message) {
        current = null;
        exportButton.setDisable(true);
        revenueChart.getData().clear();
        ticketsChart.getData().clear();
        bookingsTable.getItems().clear();
        status.setText(message);
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
        current = data;
        exportButton.setDisable(data.isEmpty());
        status.setText(data.isEmpty() ? "Không có đơn đã xác nhận trong khoảng ngày đã chọn."
                : "Tổng cộng " + data.totalBookings() + " đơn · " + data.totalTickets()
                + " vé · " + MoneyUtil.format(data.totalRevenue()));
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

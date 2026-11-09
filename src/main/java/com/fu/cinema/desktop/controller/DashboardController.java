package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.DashboardData;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DashboardController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DashboardFacade facade;
    private final FxAsync fxAsync;

    @FXML private VBox root;
    @FXML private Label lblShowing;
    @FXML private Label lblCustomers;
    @FXML private Label lblPending;
    @FXML private Label lblUpdated;
    @FXML private BarChart<String, Number> chartRooms;
    @FXML private Button btnRefresh;

    @FXML
    private void initialize() {
        chartRooms.setAnimated(false);
        refresh();
    }

    @FXML
    public void refresh() {
        fxAsync.run(facade::load, this::render, btnRefresh);
    }

    private void render(DashboardData data) {
        lblShowing.setText(String.valueOf(data.showingMovies()));
        lblCustomers.setText(String.valueOf(data.customers()));
        lblPending.setText(String.valueOf(data.pendingBookings()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Suất chiếu hôm nay");
        data.todayByRoom().forEach(r -> series.getData().add(new XYChart.Data<>(r.roomName(), r.count())));
        chartRooms.getData().setAll(List.of(series));

        lblUpdated.setText("Cập nhật lúc: " + LocalTime.now().format(TIME_FORMATTER));
    }
}

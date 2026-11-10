package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.DashboardData;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller cho màn hình Dashboard tổng quan (TODO 37 - FXML: {@code dashboard.fxml}).
 * <p>
 * Tính năng:
 * <ul>
 *   <li>Hiển thị 3 thẻ KPI: Phim đang chiếu, Tổng khách hàng, Đơn chờ duyệt (PENDING)</li>
 *   <li>Vẽ biểu đồ {@link BarChart} phân bổ suất chiếu hôm nay theo từng phòng chiếu (Q11)</li>
 *   <li>Tự động làm mới số liệu mỗi 60 giây bằng {@link Timeline} ngầm</li>
 *   <li>Lắng nghe vòng đời scene để tự dừng {@link Timeline} khi rời khỏi màn hình tránh rò rỉ bộ nhớ</li>
 *   <li>Thực hiện các lệnh gọi dữ liệu bất đồng bộ qua {@link FxAsync} không gây nghẽn luồng UI JavaFX</li>
 * </ul>
 * </p>
 */
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

    private Timeline autoRefresh;

    /**
     * Khởi tạo giao diện, thiết lập chu kỳ tự động làm mới và tải dữ liệu lần đầu.
     */
    @FXML
    private void initialize() {
        chartRooms.setAnimated(false);

        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(60), e -> refresh()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();

        // Rời màn hình (center bị thay) -> node không còn thuộc Scene -> dừng Timeline
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoRefresh != null) {
                autoRefresh.stop();
            }
        });

        refresh();
    }

    /**
     * Tải lại các chỉ số thống kê và dữ liệu biểu đồ trên Virtual Thread.
     */
    @FXML
    public void refresh() {
        fxAsync.run(facade::load, this::render, btnRefresh);
    }

    /**
     * Cập nhật số liệu KPI và vẽ lại dữ liệu cột biểu đồ trên FX Application Thread.
     *
     * @param data đối tượng {@link DashboardData} đã được facade nạp từ CSDL
     */
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

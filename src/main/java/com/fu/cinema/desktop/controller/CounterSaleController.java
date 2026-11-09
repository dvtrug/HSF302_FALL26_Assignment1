package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.component.SeatMapView;
import com.fu.cinema.desktop.facade.CounterSaleFacade;
import com.fu.cinema.desktop.facade.SeatMapFacade;
import com.fu.cinema.desktop.model.CounterMovieItem;
import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class CounterSaleController {

    private final CounterSaleFacade sales;
    private final SeatMapFacade seats;
    private final FxAsync async;

    @FXML private VBox root;
    @FXML private HBox form;
    @FXML private TextField customerQuery;
    @FXML private ListView<CustomerItem> customerResults;
    @FXML private ComboBox<CounterMovieItem> movie;
    @FXML private ComboBox<ShowtimeItem> showtime;
    @FXML private CheckBox paid;
    @FXML private Label total;
    @FXML private Label status;
    @FXML private Label seatHint;
    @FXML private Button sellButton;
    @FXML private ProgressIndicator progress;
    @FXML private SeatMapView seatMap;

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(300));
    private final BooleanProperty seatsReady = new SimpleBooleanProperty();
    private final BooleanProperty selling = new SimpleBooleanProperty();
    private final IntegerProperty reads = new SimpleIntegerProperty();
    private FxAsync.TaskGroup readsForView;
    private long customerRequest;
    private long movieRequest;
    private long seatRequest;

    @FXML
    private void initialize() {
        readsForView = async.forView(root);
        root.sceneProperty().addListener((observable, previous, scene) -> {
            if (previous != null && scene == null) {
                searchDelay.stop();
            }
        });
        bindControls();
        customerQuery.textProperty().addListener((observable, previous, keyword) -> queueCustomerSearch(keyword));
        movie.valueProperty().addListener((observable, previous, selected) -> loadShowtimes(selected));
        showtime.valueProperty().addListener((observable, previous, selected) -> loadSeats(selected));
        read(sales::movies, items -> movie.getItems().setAll(items), AlertUtil::showError);
    }

    private void bindControls() {
        var count = Bindings.size(seatMap.getSelectedSeats());
        var customer = customerResults.getSelectionModel().selectedItemProperty();
        form.disableProperty().bind(selling);
        showtime.disableProperty().bind(Bindings.isEmpty(showtime.getItems()));
        seatMap.visibleProperty().bind(seatsReady);
        seatMap.managedProperty().bind(seatsReady);
        seatHint.visibleProperty().bind(seatsReady.not());
        seatHint.managedProperty().bind(seatHint.visibleProperty());
        progress.visibleProperty().bind(reads.greaterThan(0).or(selling));
        progress.managedProperty().bind(progress.visibleProperty());
        sellButton.disableProperty().bind(customer.isNull().or(showtime.valueProperty().isNull())
                .or(seatsReady.not()).or(count.isEqualTo(0)));
        total.textProperty().bind(Bindings.createStringBinding(() -> {
            ShowtimeItem selected = showtime.getValue();
            BigDecimal amount = selected == null || !seatsReady.get() ? BigDecimal.ZERO
                    : selected.price().multiply(BigDecimal.valueOf(count.get()));
            return (seatsReady.get() ? count.get() : 0) + " ghế · " + MoneyUtil.format(amount);
        }, count, showtime.valueProperty(), seatsReady));
    }

    private void queueCustomerSearch(String keyword) {
        long request = ++customerRequest;
        searchDelay.stop();
        customerResults.getSelectionModel().clearSelection();
        customerResults.getItems().clear();
        if (keyword.isBlank()) {
            customerResults.setPlaceholder(new Label("Nhập từ khóa để tìm khách hàng."));
            return;
        }
        customerResults.setPlaceholder(new Label("Đang tìm khách hàng…"));
        searchDelay.setOnFinished(event -> read(() -> sales.searchCustomers(keyword), results -> {
            if (request == customerRequest) {
                customerResults.getItems().setAll(results);
                customerResults.setPlaceholder(new Label("Không tìm thấy khách hàng."));
            }
        }, error -> {
            if (request == customerRequest) {
                customerResults.setPlaceholder(new Label("Không thể tải khách hàng."));
                AlertUtil.showError(error);
            }
        }));
        searchDelay.playFromStart();
    }

    private void loadShowtimes(CounterMovieItem selected) {
        long request = ++movieRequest;
        showtime.setValue(null);
        showtime.getItems().clear();
        if (selected == null) {
            return;
        }
        read(() -> sales.showtimes(selected.id()), items -> {
            if (request == movieRequest) {
                showtime.getItems().setAll(items);
                showtime.setPromptText(items.isEmpty() ? "Phim chưa có suất mở bán" : "Chọn suất chiếu");
            }
        }, error -> {
            if (request == movieRequest) {
                AlertUtil.showError(error);
            }
        });
    }

    private void loadSeats(ShowtimeItem selected) {
        long request = ++seatRequest;
        seatsReady.set(false);
        seatHint.setText(selected == null ? "Chọn suất chiếu để hiển thị sơ đồ ghế." : "Đang tải sơ đồ ghế…");
        if (selected == null) {
            return;
        }
        read(() -> seats.load(selected.id()), data -> {
            if (request == seatRequest) {
                seatMap.setData(data);
                seatsReady.set(true);
            }
        }, error -> {
            if (request == seatRequest) {
                seatHint.setText("Không tải được sơ đồ ghế. Hãy chọn lại suất chiếu.");
                AlertUtil.showError(error);
            }
        });
    }

    @FXML
    private void onSell() {
        var customer = customerResults.getSelectionModel().getSelectedItem();
        var selected = showtime.getValue();
        var selectedSeats = List.copyOf(seatMap.getSelectedSeats());
        if (selling.get() || !seatsReady.get() || customer == null || selected == null || selectedSeats.isEmpty()) {
            return;
        }
        boolean paidNow = paid.isSelected();
        selling.set(true);
        status.setText("Đang tạo đơn…");
        async.run(() -> sales.sell(customer.id(), selected.id(), selectedSeats, paidNow), result -> {
            if (root.getScene() != null) {
                paid.setSelected(false);
                status.setText("Đã tạo đơn #" + result.bookingId() + " · " + result.status());
                loadSeats(selected);
                AlertUtil.info(status.getText() + " · " + MoneyUtil.format(result.total()));
            }
            selling.set(false);
        }, error -> {
            if (root.getScene() != null) {
                status.setText("Không thể tạo đơn. Sơ đồ ghế đang được cập nhật.");
                loadSeats(selected);
                AlertUtil.showError(error);
            }
            selling.set(false);
        }, null);
    }

    private <T> void read(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        reads.set(reads.get() + 1);
        readsForView.run(work, value -> {
            reads.set(reads.get() - 1);
            success.accept(value);
        }, error -> {
            reads.set(reads.get() - 1);
            failure.accept(error);
        });
    }
}

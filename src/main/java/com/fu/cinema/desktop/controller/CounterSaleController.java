package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.component.SeatMapView;
import com.fu.cinema.desktop.facade.CounterSaleFacade;
import com.fu.cinema.desktop.facade.SeatMapFacade;
import com.fu.cinema.desktop.model.CounterMovieItem;
import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.animation.PauseTransition;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import com.fu.cinema.desktop.model.SaleResult;

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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class CounterSaleController {

    private final CounterSaleFacade sales;
    private final SeatMapFacade seats;
    private final FxAsync async;
    private final DesktopSession session;

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
    @FXML private Button printButton;

    private final Timeline seatAutoRefresh = new Timeline(
            new KeyFrame(Duration.seconds(10), event -> refreshSeatsQuietly())
    );
    private SaleResult lastSale;
    private CustomerItem lastCustomer;
    private ShowtimeItem lastShowtime;


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
            if (scene != null) {
                seatAutoRefresh.setCycleCount(Animation.INDEFINITE);
                seatAutoRefresh.play();
            } else if (previous != null) {
                searchDelay.stop();
                seatAutoRefresh.stop();
            }
        });
        bindControls();
        movie.valueProperty().addListener((observable, previous, selected) -> loadShowtimes(selected));
        showtime.valueProperty().addListener((observable, previous, selected) -> loadSeats(selected));
        read(sales::movies, items -> movie.getItems().setAll(items), AlertUtil::showError);
        if (session.getUser() != null && session.getUser().isCustomer()) {
            CustomerItem me = sales.getCurrentCustomer();
            if (me != null) {
                customerQuery.setText(me.fullName());
                customerQuery.setDisable(true);
                customerResults.getItems().setAll(me);
                customerResults.getSelectionModel().selectFirst();
                customerResults.setDisable(true);
            }
            if (paid != null) {
                paid.setSelected(false);
                paid.setVisible(false);
                paid.setManaged(false);
            }
            if (sellButton != null) {
                sellButton.setText("🎟️ Đặt vé ngay");
            }
        } else {
            customerQuery.textProperty().addListener((observable, previous, keyword) -> queueCustomerSearch(keyword));
        }
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
                .or(seatsReady.not()).or(count.isEqualTo(0)).or(selling));
        total.textProperty().bind(Bindings.createStringBinding(() -> {
            ShowtimeItem selected = showtime.getValue();
            BigDecimal amount = selected == null || !seatsReady.get() ? BigDecimal.ZERO
                    : selected.price().multiply(BigDecimal.valueOf(count.get()));
            return (seatsReady.get() ? count.get() : 0) + " ghế · " + MoneyUtil.format(amount);
        }, count, showtime.valueProperty(), seatsReady));
    }

    private void queueCustomerSearch(String keyword) {
        if (session.getUser() != null && session.getUser().isCustomer()) {
            return;
        }
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
        seatMap.setData(new SeatMapData(List.of(), 0, Set.of()));
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
        String confirmation = "Bán vé cho " + customer.fullName() + "?\nGhế: " + String.join(", ", selectedSeats)
                + "\nTổng tiền: " + MoneyUtil.format(selected.price().multiply(BigDecimal.valueOf(selectedSeats.size())))
                + "\nTrạng thái: " + (paidNow ? "CONFIRMED (đã thanh toán)" : "PENDING (chưa thanh toán)");
        if (!AlertUtil.confirm(confirmation) || root.getScene() == null) {
            selling.set(false);
            return;
        }
        status.setText("Đang tạo đơn…");
        async.run(() -> sales.sell(customer.id(), selected.id(), selectedSeats, paidNow), result -> {
            if (root.getScene() != null) {
                paid.setSelected(false);
                status.setText("Đã tạo đơn #" + result.bookingId() + " · " + result.status());
                lastSale = result;
                lastCustomer = customer;
                lastShowtime = selected;
                if (printButton != null) {
                    printButton.setDisable(false);
                }
                loadSeats(selected);
                AlertUtil.info(status.getText() + "\nGhế: " + String.join(", ", result.seats())
                        + "\nTổng tiền: " + MoneyUtil.format(result.total()));
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

    private void refreshSeatsQuietly() {
        var selected = showtime.getValue();
        if (selected == null || !seatsReady.get() || selling.get()) {
            return;
        }
        var currentlySelected = List.copyOf(seatMap.getSelectedSeats());
        async.run(() -> seats.load(selected.id()), data -> {
            if (root.getScene() != null && showtime.getValue() != null && showtime.getValue().id().equals(selected.id())) {
                seatMap.setData(data);
                for (String code : currentlySelected) {
                    if (!data.soldSeats().contains(code)) {
                        seatMap.getSelectedSeats().add(code);
                    }
                }
            }
        }, null, null);
    }

    @FXML
    private void onPrint() {
        if (lastSale == null || lastCustomer == null || lastShowtime == null) {
            AlertUtil.warn("Chưa có đơn vé gần nhất để in.");
            return;
        }
        try {
            javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
            if (job == null) {
                AlertUtil.info("Không tìm thấy máy in. Hệ thống đã lưu vé #" + lastSale.bookingId() + " an toàn.");
                return;
            }
            VBox ticket = new VBox(6);
            ticket.setStyle("-fx-padding: 16; -fx-background-color: white; -fx-font-family: 'Segoe UI', sans-serif;");
            ticket.setPrefWidth(280);

            Label title = new Label("FU CINEMA - VÉ XEM PHIM");
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e50914;");
            Label dash = new Label("--------------------------------");
            Label bId = new Label("Mã đơn: #" + lastSale.bookingId() + " (" + lastSale.status() + ")");
            Label cName = new Label("Khách hàng: " + lastCustomer.fullName() + " (" + lastCustomer.mobile() + ")");
            Label mTitle = new Label("Phim: " + (movie.getValue() != null ? movie.getValue().title() : ""));
            Label sTime = new Label("Suất: " + lastShowtime.toString());
            Label sList = new Label("Ghế: " + String.join(", ", lastSale.seats()));
            sList.setStyle("-fx-font-weight: bold;");
            Label totalLbl = new Label("Tổng tiền: " + MoneyUtil.format(lastSale.total()));
            totalLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            Label footer = new Label("\nChúc quý khách xem phim vui vẻ!");
            footer.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

            ticket.getChildren().addAll(title, dash, bId, cName, mTitle, sTime, sList, totalLbl, footer);

            if (root.getScene() != null && root.getScene().getWindow() != null) {
                boolean proceed = job.showPrintDialog(root.getScene().getWindow());
                if (proceed) {
                    if (job.printPage(ticket)) {
                        job.endJob();
                        AlertUtil.info("Đã in vé thành công cho đơn #" + lastSale.bookingId() + "!");
                    } else {
                        job.cancelJob();
                    }
                }
            }
        } catch (Exception e) {
            AlertUtil.showError(e);
        }
    }
}

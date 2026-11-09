package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.BookingFacade;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.AlertUtil;
import javafx.beans.binding.Bindings;
import com.fu.cinema.enums.BookingStatus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class BookingAdminController {
    private final BookingFacade facade;
    private final FxAsync async;
    @FXML private VBox root;
    @FXML private ComboBox<BookingStatus> cbStatus;
    @FXML private DatePicker dpFrom, dpTo;
    @FXML private TextField tfKeyword;
    @FXML private TableView<BookingRow> table;
    @FXML private TableColumn<BookingRow, Long> colId;
    @FXML private TableColumn<BookingRow, String> colCustomer, colMovie, colRoom;
    @FXML private TableColumn<BookingRow, LocalDateTime> colStart, colDate;
    @FXML private TableColumn<BookingRow, Integer> colSeats;
    @FXML private TableColumn<BookingRow, BigDecimal> colTotal;
    @FXML private TableColumn<BookingRow, BookingStatus> colStatus;
    @FXML private Button btnConfirm, btnCancel;
    @FXML private ProgressIndicator progress;
    @FXML private Pagination pagination;
    @FXML private Label lblError, lblTotal;
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private FxAsync.TaskGroup tasks;

    @FXML private void initialize() {
        tasks = async.forView(root);
        cbStatus.getItems().add(null);
        cbStatus.getItems().addAll(BookingStatus.values());
        progress.visibleProperty().bind(busy);
        progress.managedProperty().bind(busy);
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(BookingStatus value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("st-pending", "st-confirmed", "st-cancelled");
                setText(empty || value == null ? null : value.name());
                if (!empty && value != null) getStyleClass().add("st-" + value.name().toLowerCase(java.util.Locale.ROOT));
            }
        });
        var selected = table.getSelectionModel().selectedItemProperty();
        btnConfirm.disableProperty().bind(Bindings.createBooleanBinding(
                () -> busy.get() || selected.get() == null || selected.get().status() != BookingStatus.PENDING,
                selected, busy));
        btnCancel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> busy.get() || selected.get() == null || selected.get().status() == BookingStatus.CANCELLED,
                selected, busy));
        table.disableProperty().bind(busy);
    }
    @FXML private void onSearch() { }
    @FXML private void onRefresh() { }
    @FXML private void onConfirm() { changeStatus(true); }
    @FXML private void onCancel() { changeStatus(false); }

    private void changeStatus(boolean confirm) {
        BookingRow row = table.getSelectionModel().getSelectedItem();
        if (row == null || busy.get()) return;
        if (!AlertUtil.confirm((confirm ? "Xác nhận" : "Huỷ") + " đơn #" + row.id() + "?")) return;
        busy.set(true);
        tasks.run(() -> {
            if (confirm) facade.confirm(row.id()); else facade.cancel(row.id());
            return row.id();
        }, id -> { busy.set(false); onRefresh(); }, ex -> { busy.set(false); AlertUtil.showError(ex); onRefresh(); });
    }
}

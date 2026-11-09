package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.RoomFacade;
import com.fu.cinema.desktop.model.RoomRow;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class RoomController {

    private final RoomFacade facade;
    private final FxAsync fxAsync;
    private final FxValidator validator;

    @FXML private HBox root;
    @FXML private Label lblRoomCount;
    @FXML private Button btnRefresh;

    @FXML private TableView<RoomRow> tableRooms;
    @FXML private TableColumn<RoomRow, Long> colId;
    @FXML private TableColumn<RoomRow, String> colName;
    @FXML private TableColumn<RoomRow, RoomType> colType;
    @FXML private TableColumn<RoomRow, Integer> colRows;
    @FXML private TableColumn<RoomRow, Integer> colSeatsPerRow;
    @FXML private TableColumn<RoomRow, Integer> colCapacity;
    @FXML private TableColumn<RoomRow, RoomStatus> colStatus;

    // Form elements
    @FXML private VBox formBox;
    @FXML private Label lblFormTitle;
    @FXML private TextField tfName;
    @FXML private ComboBox<RoomType> cbType;
    @FXML private Spinner<Integer> spRows;
    @FXML private Spinner<Integer> spSeatsPerRow;
    @FXML private Label lblCalculatedCapacity;
    @FXML private ComboBox<RoomStatus> cbStatus;
    @FXML private Button btnNew;
    @FXML private Button btnDelete;
    @FXML private Button btnSave;

    private final ObservableList<RoomRow> rows = FXCollections.observableArrayList();
    private Long editingId;

    @FXML
    private void initialize() {
        // Cấu hình các cột TableView theo chuẩn lambda record (TECH-14)
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        colType.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().type()));
        colRows.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().rows()));
        colSeatsPerRow.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().seatsPerRow()));
        colCapacity.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().capacity()));
        colCapacity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : val + " ghế");
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(RoomStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.name());
                    if (status == RoomStatus.ACTIVE) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tableRooms.setItems(rows);

        // Khởi tạo ComboBox và Spinner
        cbType.getItems().setAll(RoomType.values());
        cbStatus.getItems().setAll(RoomStatus.values());

        spRows.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 26, 10));
        spSeatsPerRow.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 12));

        // Tự động tính sức chứa qua Binding
        lblCalculatedCapacity.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    Integer r = spRows.getValue();
                    Integer s = spSeatsPerRow.getValue();
                    int total = (r != null ? r : 0) * (s != null ? s : 0);
                    return total + " ghế";
                },
                spRows.valueProperty(), spSeatsPerRow.valueProperty()));

        // Lắng nghe chọn dòng trên bảng để đổ dữ liệu vào form
        tableRooms.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, row) -> fill(row));

        // Binding bật/tắt nút hành động
        btnDelete.disableProperty().bind(tableRooms.getSelectionModel().selectedItemProperty().isNull());
        btnSave.disableProperty().bind(tfName.textProperty().isEmpty());

        onNew();
    }

    private void fill(RoomRow row) {
        if (row == null) {
            onNew();
            return;
        }
        editingId = row.id();
        lblFormTitle.setText("Chỉnh sửa phòng: " + row.name());
        tfName.setText(row.name());
        cbType.setValue(row.type());
        spRows.getValueFactory().setValue(row.rows());
        spSeatsPerRow.getValueFactory().setValue(row.seatsPerRow());
        cbStatus.setValue(row.status());
    }

    @FXML
    public void onNew() {
        editingId = null;
        lblFormTitle.setText("Thêm phòng chiếu mới");
        tfName.clear();
        cbType.setValue(RoomType.TWO_D);
        spRows.getValueFactory().setValue(10);
        spSeatsPerRow.getValueFactory().setValue(12);
        cbStatus.setValue(RoomStatus.ACTIVE);
        tableRooms.getSelectionModel().clearSelection();
    }

    @FXML
    public void reload() {
    }

    @FXML
    public void onSave() {
    }

    @FXML
    public void onDelete() {
    }
}

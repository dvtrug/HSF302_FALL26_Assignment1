package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.RoomRow;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
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

    @FXML
    public void reload() {
    }

    @FXML
    public void onNew() {
    }

    @FXML
    public void onSave() {
    }

    @FXML
    public void onDelete() {
    }
}

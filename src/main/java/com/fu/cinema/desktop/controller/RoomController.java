package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.RoomRow;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
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

    @FXML
    public void reload() {
    }
}

package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.ShowtimeFacade;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.enums.ShowtimeStatus;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Controller managing the showtime scheduling tab (showtime-tab.fxml).
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeFacade facade;

    @FXML
    private HBox root;

    // Filter controls
    @FXML
    private ComboBox<MovieItem> cbFilterMovie;

    @FXML
    private ComboBox<RoomItem> cbFilterRoom;

    @FXML
    private DatePicker dpFilterDate;

    @FXML
    private Button btnFilter;

    @FXML
    private Button btnResetFilter;

    @FXML
    private Button btnRefresh;

    // Table and columns
    @FXML
    private TableView<ShowtimeRow> tableShowtimes;

    @FXML
    private TableColumn<ShowtimeRow, Long> colId;

    @FXML
    private TableColumn<ShowtimeRow, String> colMovie;

    @FXML
    private TableColumn<ShowtimeRow, String> colRoom;

    @FXML
    private TableColumn<ShowtimeRow, LocalDateTime> colStart;

    @FXML
    private TableColumn<ShowtimeRow, LocalDateTime> colEnd;

    @FXML
    private TableColumn<ShowtimeRow, BigDecimal> colPrice;

    @FXML
    private TableColumn<ShowtimeRow, ShowtimeStatus> colStatus;

    // Pagination
    @FXML
    private Button btnPrevPage;

    @FXML
    private Button btnNextPage;

    @FXML
    private Label lblPageInfo;

    // Form container
    @FXML
    private VBox formBox;

    @FXML
    private Label lblFormTitle;

    @FXML
    private void initialize() {
        // Initial setup for filter controls and table columns
    }

    @FXML
    public void onFilter() {
    }

    @FXML
    public void onResetFilter() {
    }

    @FXML
    public void reload() {
    }

    @FXML
    public void onPrevPage() {
    }

    @FXML
    public void onNextPage() {
    }
}

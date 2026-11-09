package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.ShowtimeFacade;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.enums.ShowtimeStatus;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
    private ComboBox<MovieItem> cbMovie;

    @FXML
    private ComboBox<RoomItem> cbRoom;

    @FXML
    private DatePicker dpDate;

    @FXML
    private Spinner<Integer> spHour;

    @FXML
    private Spinner<Integer> spMinute;

    @FXML
    private TextField tfPrice;

    @FXML
    private Label lblEnd;

    @FXML
    private Button btnNew;

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnSave;

    private Long editingId;

    @FXML
    private void initialize() {
        // Setup spinners and text formatter for input form
        spHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 19));
        spMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        tfPrice.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,9}") ? c : null));

        // Disable past dates in date picker
        dpDate.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });

        // Bind suggested EndTime calculation: StartTime + Movie Duration + 15' room cleanup
        DateTimeFormatter endFormatter = DateTimeFormatter.ofPattern("HH:mm dd/MM");
        lblEnd.textProperty().bind(Bindings.createStringBinding(() -> {
            LocalDateTime end = suggestedEnd();
            return end == null ? "Kết thúc (gợi ý): -" : "Kết thúc (gợi ý): " + end.format(endFormatter);
        }, cbMovie.valueProperty(), dpDate.valueProperty(), spHour.valueProperty(), spMinute.valueProperty()));

        // Commit spinner values on focus lost
        spHour.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) spHour.commitValue();
        });
        spMinute.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) spMinute.commitValue();
        });
    }

    public LocalDateTime startTime() {
        if (dpDate.getValue() == null || spHour.getValue() == null || spMinute.getValue() == null) {
            return null;
        }
        return LocalDateTime.of(dpDate.getValue(), LocalTime.of(spHour.getValue(), spMinute.getValue()));
    }

    public LocalDateTime suggestedEnd() {
        LocalDateTime start = startTime();
        if (start == null || cbMovie.getValue() == null) {
            return null;
        }
        return start.plusMinutes(cbMovie.getValue().duration() + 15L);
    }

    @FXML
    public void onNew() {
    }

    @FXML
    public void onDelete() {
    }

    @FXML
    public void onSave() {
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

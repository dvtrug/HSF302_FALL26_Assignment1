package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.ShowtimeFacade;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.enums.ShowtimeStatus;
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller managing the showtime scheduling tab ({@code showtime-tab.fxml}).
 * <p>
 * <b>Key Responsibilities:</b>
 * <ul>
 *   <li>Presents paged showtime schedules with multi-criteria filtering (Movie, Room, Date).</li>
 *   <li>Dynamically calculates and binds suggested end times:
 *       {@code EndTime = StartTime + MovieDuration + 15' room cleanup}.</li>
 *   <li>Performs client-side validation using {@link FxValidator} before dispatching save requests.</li>
 *   <li>Executes database operations asynchronously off the UI thread via {@link FxAsync}.</li>
 *   <li>Handles schedule conflict notifications (BR-08) and cascading cancellation feedback (BR-09).</li>
 * </ul>
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ShowtimeController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));

    private final ShowtimeFacade facade;
    private final DesktopSession session;
    private final FxValidator validator;
    private final FxAsync fxAsync;

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

    private final ObservableList<ShowtimeRow> showtimeRows = FXCollections.observableArrayList();
    private Long editingId;
    private int currentPage = 0;
    private int totalPages = 1;

    @FXML
    private void initialize() {
        // Cấu hình TableColumn theo chuẩn lambda record (TECH-14)
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colMovie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().movieTitle()));
        colRoom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roomName()));

        colStart.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().start()));
        colStart.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : dt.format(DATE_TIME_FORMATTER));
            }
        });

        colEnd.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().end()));
        colEnd.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : dt.format(DATE_TIME_FORMATTER));
            }
        });

        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().price()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : CURRENCY_FORMATTER.format(price));
            }
        });

        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ShowtimeStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.name());
                    if (status == ShowtimeStatus.OPEN) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tableShowtimes.setItems(showtimeRows);

        // Setup Spinners and TextFormatter
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

        // Lắng nghe chọn dòng trên bảng để đổ dữ liệu vào form
        tableShowtimes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, row) -> fill(row));

        // Binding nút xóa disable khi không chọn dòng
        btnDelete.disableProperty().bind(tableShowtimes.getSelectionModel().selectedItemProperty().isNull());

        loadDropdownData();
        onNew();
        loadPage(0);
    }

    /**
     * Tải danh sách phim và phòng chiếu vào các ComboBox filter và form.
     */
    private void loadDropdownData() {
        fxAsync.run(() -> {
            List<MovieItem> movies = facade.schedulableMovies();
            List<RoomItem> rooms = facade.activeRooms();
            return new DropdownData(movies, rooms);
        }, data -> {
            cbFilterMovie.getItems().setAll(data.movies());
            cbMovie.getItems().setAll(data.movies());

            cbFilterRoom.getItems().setAll(data.rooms());
            cbRoom.getItems().setAll(data.rooms());
        }, null);
    }

    /**
     * Tải dữ liệu trang suất chiếu theo bộ lọc hiện tại.
     *
     * @param page số trang (bắt đầu từ 0)
     */
    public void loadPage(int page) {
        Long movieId = cbFilterMovie.getValue() != null ? cbFilterMovie.getValue().id() : null;
        Long roomId = cbFilterRoom.getValue() != null ? cbFilterRoom.getValue().id() : null;
        LocalDate date = dpFilterDate.getValue();

        fxAsync.run(
                () -> facade.search(movieId, roomId, date, page),
                result -> {
                    currentPage = result.page();
                    totalPages = result.totalPages();
                    showtimeRows.setAll(result.items());
                    lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages);
                    btnPrevPage.setDisable(currentPage <= 0);
                    btnNextPage.setDisable(currentPage + 1 >= totalPages);
                },
                root
        );
    }

    /**
     * Điền thông tin suất chiếu từ TableView vào form nhập liệu.
     */
    private void fill(ShowtimeRow row) {
        if (row == null) {
            return;
        }
        editingId = row.id();
        lblFormTitle.setText("Cập nhật suất chiếu #" + row.id());

        // Chọn movie tương ứng
        if (cbMovie.getItems() != null) {
            cbMovie.getItems().stream()
                    .filter(m -> m.title().equals(row.movieTitle()))
                    .findFirst()
                    .ifPresent(cbMovie::setValue);
        }

        // Chọn room tương ứng
        if (cbRoom.getItems() != null) {
            cbRoom.getItems().stream()
                    .filter(r -> r.name().equals(row.roomName()))
                    .findFirst()
                    .ifPresent(cbRoom::setValue);
        }

        if (row.start() != null) {
            dpDate.setValue(row.start().toLocalDate());
            spHour.getValueFactory().setValue(row.start().getHour());
            spMinute.getValueFactory().setValue(row.start().getMinute());
        }

        if (row.price() != null) {
            tfPrice.setText(row.price().toPlainString());
        } else {
            tfPrice.clear();
        }
    }

    @FXML
    public void onFilter() {
        loadPage(0);
    }

    @FXML
    public void onResetFilter() {
        cbFilterMovie.setValue(null);
        cbFilterRoom.setValue(null);
        dpFilterDate.setValue(null);
        loadPage(0);
    }

    @FXML
    public void reload() {
        loadDropdownData();
        loadPage(currentPage);
    }

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) {
            loadPage(currentPage - 1);
        }
    }

    @FXML
    public void onNextPage() {
        if (currentPage + 1 < totalPages) {
            loadPage(currentPage + 1);
        }
    }

    @FXML
    public void onNew() {
        editingId = null;
        tableShowtimes.getSelectionModel().clearSelection();
        lblFormTitle.setText("Thêm suất chiếu mới");
        cbMovie.setValue(null);
        cbRoom.setValue(null);
        dpDate.setValue(LocalDate.now());
        spHour.getValueFactory().setValue(19);
        spMinute.getValueFactory().setValue(0);
        tfPrice.clear();
        validator.mark(Map.of(), Map.of(
                "movieId", cbMovie,
                "roomId", cbRoom,
                "startTime", dpDate,
                "ticketPrice", tfPrice
        ));
    }

    /**
     * Xóa hoặc hủy suất chiếu theo quy tắc an toàn BR-09.
     */
    @FXML
    public void onDelete() {
        ShowtimeRow selected = tableShowtimes.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!AlertUtil.confirm("Bạn có chắc chắn muốn xoá suất chiếu #" + selected.id() + " của phim \"" + selected.movieTitle() + "\"?")) {
            return;
        }

        fxAsync.run(
                () -> facade.delete(selected.id()),
                deleted -> {
                    if (deleted) {
                        AlertUtil.info("Đã xoá vĩnh viễn suất chiếu #" + selected.id() + "!");
                    } else {
                        AlertUtil.warn("Suất chiếu đã có vé đặt → Đã huỷ suất chiếu và huỷ các đơn vé liên quan theo quy định BR-09.");
                    }
                    onNew();
                    loadPage(currentPage);
                },
                formBox
        );
    }

    /**
     * Lưu suất chiếu (thêm mới hoặc cập nhật) với kiểm tra trùng lịch BR-08 và tính giờ kết thúc.
     */
    @FXML
    public void onSave() {
        // Đồng bộ giá trị spinner nếu người dùng gõ tay
        spHour.commitValue();
        spMinute.commitValue();

        ShowtimeForm form = new ShowtimeForm();
        form.setShowtimeId(editingId);
        form.setMovieId(cbMovie.getValue() == null ? null : cbMovie.getValue().id());
        form.setRoomId(cbRoom.getValue() == null ? null : cbRoom.getValue().id());
        form.setStartTime(startTime());
        form.setEndTime(suggestedEnd());
        form.setTicketPrice(tfPrice.getText() == null || tfPrice.getText().isBlank() ? null : new BigDecimal(tfPrice.getText().trim()));
        form.setStatus(ShowtimeStatus.OPEN);

        Map<String, String> errors = validator.validate(form);
        if (!validator.mark(errors, Map.of(
                "movieId", cbMovie,
                "roomId", cbRoom,
                "startTime", dpDate,
                "ticketPrice", tfPrice))) {
            return;
        }

        fxAsync.runVoid(
                () -> facade.save(form),
                () -> {
                    AlertUtil.info("Đã lưu suất chiếu thành công!");
                    onNew();
                    loadPage(currentPage);
                },
                formBox
        );
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

    private record DropdownData(List<MovieItem> movies, List<RoomItem> rooms) {}
}

package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.model.MovieRow;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.MovieStatus;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final javafx.css.PseudoClass STOPPED = javafx.css.PseudoClass.getPseudoClass("stopped");
    private final javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));

    private final MovieFacade movieFacade;
    private final FxAsync fxAsync;
    private final SceneManager sceneManager;

    @FXML private VBox root;
    @FXML private ProgressIndicator progress;
    @FXML private TextField tfKeyword;
    @FXML private ComboBox<GenreItem> cbGenre;
    @FXML private ComboBox<MovieStatus> cbStatus;
    @FXML private Button btnReset;
    @FXML private Button btnNew;
    @FXML private Label lblTotal;

    @FXML private TableView<MovieRow> tableMovies;
    @FXML private TableColumn<MovieRow, Long> colId;
    @FXML private TableColumn<MovieRow, String> colTitle;
    @FXML private TableColumn<MovieRow, String> colGenre;
    @FXML private TableColumn<MovieRow, String> colDuration;
    @FXML private TableColumn<MovieRow, String> colReleaseDate;
    @FXML private TableColumn<MovieRow, String> colAgeRating;
    @FXML private TableColumn<MovieRow, MovieStatus> colStatus;
    @FXML private TableColumn<MovieRow, Void> colActions;

    @FXML private Pagination pagination;

    private final ObservableList<MovieRow> movieList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupColumns();
        loadFilters();
        setupSearchDebounce();

        btnNew.setOnAction(e -> openDialog(null));

        pagination.setPageFactory(pageIndex -> {
            loadPage(pageIndex);
            return new Label(); // Dummy node, bảng cập nhật trực tiếp qua movieList
        });
    }

    public void loadPage(int page) {
        progress.setVisible(true);
        String kw = tfKeyword.getText() != null ? tfKeyword.getText().trim() : "";
        Long genreId = cbGenre.getValue() != null ? cbGenre.getValue().genreId() : null;
        MovieStatus st = cbStatus.getValue();
        fxAsync.run(
                () -> movieFacade.search(kw, genreId, st, page, 10),
                res -> {
                    progress.setVisible(false);
                    movieList.setAll(res.items());
                    pagination.setPageCount(res.totalPages());
                    lblTotal.setText("Tổng số: " + res.totalElements() + " phim");
                },
                ex -> {
                    progress.setVisible(false);
                    AlertUtil.showError(ex);
                },
                null
        );
    }

    private void setupColumns() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().movieId()));
        colTitle.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().title()));
        colGenre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().genreName()));
        colDuration.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().duration() + " phút"));
        colReleaseDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().releaseDate() != null ? c.getValue().releaseDate().format(DATE_FMT) : ""
        ));
        colAgeRating.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().ageRating() != null ? c.getValue().ageRating().name() : ""
        ));
        colStatus.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().status()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Sửa");
            private final Button btnDelete = new Button("Xoá");
            private final HBox box = new HBox(6, btnEdit, btnDelete);

            {
                btnDelete.getStyleClass().add("button-danger");

                btnEdit.setOnAction(e -> {
                    MovieRow row = getTableView().getItems().get(getIndex());
                    openDialog(row.movieId());
                });

                btnDelete.setOnAction(e -> {
                    MovieRow row = getTableView().getItems().get(getIndex());
                    onDeleteMovie(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableMovies.setItems(movieList);
        tableMovies.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(MovieRow item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(STOPPED, !empty && item != null && item.status() == MovieStatus.STOPPED);
            }
        });
    }

    public void openDialog(Long movieId) {
        var loaded = sceneManager.load(View.MOVIE_DIALOG);
        MovieDialogController controller = (MovieDialogController) loaded.controller();

        if (movieId != null) {
            controller.setForm(movieFacade.getForm(movieId));
        } else {
            controller.setForm(null);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(movieId == null ? "Thêm phim mới" : "Chỉnh sửa phim #" + movieId);
        dialog.getDialogPane().setContent(loaded.root());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Áp dụng stylesheet desktop.css cho Dialog để hiển thị viền đỏ :error và thông báo
        String css = java.util.Objects.requireNonNull(getClass().getResource("/desktop/css/desktop.css")).toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);

        // Chặn đóng Dialog nếu validate lỗi (TC-D09)
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!controller.validate()) {
                event.consume(); // Chặn đóng dialog
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                MovieForm form = controller.buildForm();
                fxAsync.run(
                        () -> {
                            movieFacade.save(form);
                            return true;
                        },
                        ok -> {
                            AlertUtil.info(movieId == null ? "Thêm phim thành công!" : "Cập nhật phim thành công!");
                            loadPage(pagination.getCurrentPageIndex());
                        }
                );
            }
        });
    }

    private void onDeleteMovie(MovieRow row) {
        boolean confirm = AlertUtil.confirm("Bạn có chắc chắn muốn xoá phim: \"" + row.title() + "\"?");
        if (!confirm) return;

        progress.setVisible(true);
        fxAsync.run(
                () -> movieFacade.delete(row.movieId()),
                hardDeleted -> {
                    progress.setVisible(false);
                    if (hardDeleted) {
                        AlertUtil.info("Đã xoá hoàn toàn phim \"" + row.title() + "\"!");
                    } else {
                        AlertUtil.warn("Phim đã có suất chiếu liên kết nên không thể xoá hẳn. Đã chuyển trạng thái sang NGỪNG CHIẾU (BR-06).");
                    }
                    loadPage(pagination.getCurrentPageIndex());
                },
                ex -> {
                    progress.setVisible(false);
                    AlertUtil.showError(ex);
                },
                null
        );
    }

    private void loadFilters() {
        cbStatus.getItems().setAll(MovieStatus.values());
        fxAsync.run(
                movieFacade::findAllGenres,
                genres -> cbGenre.getItems().setAll(genres)
        );
    }

    @FXML
    public void onResetFilters() {
        tfKeyword.clear();
        cbGenre.setValue(null);
        cbStatus.setValue(null);
    }

    private void setupSearchDebounce() {
        debounce.setOnFinished(e -> loadPage(0));
        tfKeyword.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());
        cbGenre.valueProperty().addListener((obs, oldVal, newVal) -> loadPage(0));
        cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> loadPage(0));
    }
}

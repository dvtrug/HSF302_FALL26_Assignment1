package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.model.MovieRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.enums.MovieStatus;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MovieFacade movieFacade;
    private final FxAsync fxAsync;

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

        tableMovies.setItems(movieList);
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
}
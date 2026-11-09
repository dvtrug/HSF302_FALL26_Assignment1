package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieDialogController {

    private final MovieFacade movieFacade;
    private final FxValidator fxValidator;

    @FXML private TextField tfTitle;
    @FXML private ComboBox<GenreItem> cbGenre;
    @FXML private TextField tfDirector;
    @FXML private Spinner<Integer> spDuration;
    @FXML private DatePicker dpReleaseDate;
    @FXML private ComboBox<AgeRating> cbAgeRating;
    @FXML private ComboBox<MovieStatus> cbStatus;
    @FXML private TextField tfPosterUrl;
    @FXML private TextArea taDescription;

    private Long currentMovieId = null;

    @FXML
    private void initialize() {
        configureControls();
    }

    private void configureControls() {
        // Cấu hình Spinner: tối thiểu 30 phút, tối đa 300 phút, mặc định 120 phút, bước nhảy 5
        SpinnerValueFactory.IntegerSpinnerValueFactory durationFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 300, 120, 5);
        spDuration.setValueFactory(durationFactory);
        spDuration.setEditable(true);

        // Nạp ComboBox
        cbAgeRating.getItems().setAll(AgeRating.values());
        cbStatus.getItems().setAll(MovieStatus.values());
        cbGenre.getItems().setAll(movieFacade.findAllGenres());
    }
}

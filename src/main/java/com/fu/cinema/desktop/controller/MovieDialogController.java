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

import java.util.Map;

/**
 * Controller điều khiển Dialog Thêm và Sửa phim (movie-dialog.fxml).
 * <p>
 * Áp dụng Event Filter trên nút OK để chặn sự kiện đóng Dialog khi form vi phạm
 * Bean Validation (TC-D09), bảo vệ toàn vẹn dữ liệu trước khi gửi tới Facade.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieDialogController {

    private final MovieFacade movieFacade;
    private final FxValidator fxValidator;

    @FXML private Label lblError;
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

    public void setForm(MovieForm form) {
        if (lblError != null) {
            lblError.setText("");
            lblError.setVisible(false);
            lblError.setManaged(false);
        }

        if (form == null) {
            currentMovieId = null;
            tfTitle.clear();
            tfDirector.clear();
            spDuration.getValueFactory().setValue(120);
            spDuration.getEditor().setText("120");
            dpReleaseDate.setValue(null);
            cbAgeRating.setValue(AgeRating.P);
            cbStatus.setValue(MovieStatus.SHOWING);
            tfPosterUrl.clear();
            taDescription.clear();
            return;
        }

        currentMovieId = form.getMovieId();
        tfTitle.setText(form.getTitle());
        tfDirector.setText(form.getDirector());
        if (form.getDuration() != null) {
            spDuration.getValueFactory().setValue(form.getDuration());
            spDuration.getEditor().setText(String.valueOf(form.getDuration()));
        }
        dpReleaseDate.setValue(form.getReleaseDate());
        cbAgeRating.setValue(form.getAgeRating());
        cbStatus.setValue(form.getStatus());
        tfPosterUrl.setText(form.getPosterUrl());
        taDescription.setText(form.getDescription());

        // Chọn Genre tương ứng
        if (form.getGenreId() != null) {
            cbGenre.getItems().stream()
                    .filter(g -> g.genreId().equals(form.getGenreId()))
                    .findFirst()
                    .ifPresent(cbGenre::setValue);
        }
    }

    public MovieForm buildForm() {
        MovieForm form = new MovieForm();
        form.setMovieId(currentMovieId);
        form.setTitle(tfTitle.getText() != null ? tfTitle.getText().trim() : "");
        form.setDirector(tfDirector.getText() != null ? tfDirector.getText().trim() : "");

        Integer duration = null;
        try {
            String text = spDuration.getEditor() != null ? spDuration.getEditor().getText() : null;
            if (text != null && !text.isBlank()) {
                duration = Integer.parseInt(text.trim());
            } else if (spDuration.getValue() != null) {
                duration = spDuration.getValue();
            }
        } catch (NumberFormatException e) {
            duration = -1; // Kích hoạt @Min(30)
        }
        form.setDuration(duration);

        form.setReleaseDate(dpReleaseDate.getValue());
        form.setAgeRating(cbAgeRating.getValue());
        form.setStatus(cbStatus.getValue());
        form.setGenreId(cbGenre.getValue() != null ? cbGenre.getValue().genreId() : null);
        form.setPosterUrl(tfPosterUrl.getText() != null ? tfPosterUrl.getText().trim() : "");
        form.setDescription(taDescription.getText() != null ? taDescription.getText().trim() : "");
        return form;
    }

    public boolean validate() {
        MovieForm form = buildForm();
        Map<String, String> errors = fxValidator.validate(form);
        Map<String, Control> fields = Map.of(
                "title", tfTitle,
                "director", tfDirector,
                "duration", spDuration,
                "releaseDate", dpReleaseDate,
                "ageRating", cbAgeRating,
                "status", cbStatus,
                "genreId", cbGenre,
                "posterUrl", tfPosterUrl,
                "description", taDescription
        );
        boolean valid = fxValidator.mark(errors, fields);
        if (!valid && lblError != null) {
            String firstError = errors.values().iterator().next();
            lblError.setText("⚠ " + firstError);
            lblError.setVisible(true);
            lblError.setManaged(true);
        } else if (lblError != null) {
            lblError.setText("");
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
        return valid;
    }
}

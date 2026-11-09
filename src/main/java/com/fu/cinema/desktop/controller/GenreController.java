package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.GenreFacade;
import com.fu.cinema.desktop.model.GenreRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.GenreForm;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class GenreController {

    private final GenreFacade genreFacade;
    private final FxAsync fxAsync;
    private final FxValidator fxValidator;

    @FXML private VBox root;
    @FXML private ProgressIndicator progress;

    @FXML private TableView<GenreRow> tableGenres;
    @FXML private TableColumn<GenreRow, Long> colId;
    @FXML private TableColumn<GenreRow, String> colName;
    @FXML private TableColumn<GenreRow, String> colDescription;

    @FXML private Label lblFormTitle;
    @FXML private TextField tfGenreName;
    @FXML private TextArea taDescription;
    @FXML private Button btnSave;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;
    @FXML private Button btnReload;

    private final ObservableList<GenreRow> genres = FXCollections.observableArrayList();
    private Long selectedGenreId = null;

    @FXML
    private void initialize() {
        setupTable();
        setupSelectionListener();
        bindButtonStates();
        onReload();
    }
    private void bindButtonStates() {
        // Khóa nút Lưu khi ô tên thể loại rỗng
        btnSave.disableProperty().bind(tfGenreName.textProperty().isEmpty());
        // Khóa nút Xóa khi chưa chọn dòng nào trên bảng
        btnDelete.setDisable(true);
        tableGenres.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            btnDelete.setDisable(selected == null);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().genreId()));
        colName.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().genreName()));
        colDescription.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().description()));
        tableGenres.setItems(genres);
    }

    private void setupSelectionListener() {
        tableGenres.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                selectedGenreId = selected.genreId();
                tfGenreName.setText(selected.genreName());
                taDescription.setText(selected.description());
                lblFormTitle.setText("Chỉnh sửa thể loại #" + selectedGenreId);
            }
        });
    }
    private GenreForm buildForm() {
        GenreForm form = new GenreForm();
        form.setGenreId(selectedGenreId);
        form.setGenreName(tfGenreName.getText() != null ? tfGenreName.getText().trim() : "");
        form.setDescription(taDescription.getText() != null ? taDescription.getText().trim() : "");
        return form;
    }

    private boolean validateForm(GenreForm form) {
        Map<String, String> errors = fxValidator.validate(form);
        Map<String, Control> fields = Map.of(
                "genreName", tfGenreName,
                "description", taDescription
        );
        return fxValidator.mark(errors, fields);
    }

    @FXML
    public void onReload() {
        genres.clear();
        genres.addAll(genreFacade.findAll());
    }

    @FXML
    public void onClearForm() {
        tableGenres.getSelectionModel().clearSelection();
        selectedGenreId = null;
        tfGenreName.clear();
        taDescription.clear();
        lblFormTitle.setText("Thêm thể loại mới");
    }

    @FXML
    public void onSaveGenre() {
        // Sẽ hoàn thiện ở commit tiếp theo
    }

    @FXML
    public void onDeleteGenre() {
        // Sẽ hoàn thiện ở commit tiếp theo
    }
}
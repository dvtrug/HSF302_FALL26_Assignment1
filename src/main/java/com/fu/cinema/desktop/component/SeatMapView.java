package com.fu.cinema.desktop.component;

import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.desktop.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;

public class SeatMapView extends VBox {

    private static final int MAX_SELECTION = 8;

    private static final PseudoClass SOLD = PseudoClass.getPseudoClass("sold");
    private final ObservableList<String> selectedSeats = FXCollections.observableArrayList();
    private final ObservableList<String> selectionView =
            FXCollections.unmodifiableObservableList(selectedSeats);
    private final Runnable onLimit;

    @FXML
    private GridPane seats;

    public SeatMapView() {
        this(() -> AlertUtil.warn("Mỗi đơn tối đa " + MAX_SELECTION + " ghế"));
    }

    SeatMapView(Runnable onLimit) {
        this.onLimit = onLimit;
        FXMLLoader loader = new FXMLLoader(SeatMapView.class.getResource("/desktop/fxml/seat-map.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Không nạp được sơ đồ ghế", e);
        }
    }

    public void setData(SeatMapData data) {
        seats.getChildren().clear();
        selectedSeats.clear();
        for (int row = 0; row < data.rowLabels().size(); row++) {
            String label = data.rowLabels().get(row);
            seats.add(new Label(label), 0, row);
            for (int number = 1; number <= data.seatsPerRow(); number++) {
                String code = label + number;
                seats.add(createSeat(code, data.soldSeats().contains(code)), number, row);
            }
        }
    }

    /** Live, read-only seat codes in selection order. */
    public ObservableList<String> getSelectedSeats() {
        return selectionView;
    }

    private ToggleButton createSeat(String code, boolean sold) {
        ToggleButton seat = new ToggleButton(code);
        seat.getStyleClass().add("seat");
        seat.pseudoClassStateChanged(SOLD, sold);
        seat.setDisable(sold);
        seat.setAccessibleText(sold ? code + " đã bán" : code);
        seat.setOnAction(event -> updateSelection(seat, code));
        return seat;
    }

    private void updateSelection(ToggleButton seat, String code) {
        if (!seat.isSelected()) {
            selectedSeats.remove(code);
        } else if (selectedSeats.size() == MAX_SELECTION) {
            seat.setSelected(false);
            onLimit.run();
        } else {
            selectedSeats.add(code);
        }
    }

}

package com.fu.cinema.desktop.component;

import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.desktop.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SeatMapView extends VBox {

    private static final int MAX_SELECTION = 8;

    private static final PseudoClass SOLD = PseudoClass.getPseudoClass("sold");
    private final GridPane seats = new GridPane();
    private final ObservableList<String> selectedSeats = FXCollections.observableArrayList();
    private final Runnable onLimit;

    public SeatMapView() {
        this(() -> AlertUtil.warn("Mỗi đơn tối đa " + MAX_SELECTION + " ghế"));
    }

    SeatMapView(Runnable onLimit) {
        super(12);
        this.onLimit = onLimit;
        setAlignment(Pos.TOP_CENTER);
        Label screen = new Label("MÀN HÌNH");
        screen.getStyleClass().add("screen");
        screen.setMaxWidth(Double.MAX_VALUE);
        seats.setAlignment(Pos.CENTER);
        seats.setHgap(5);
        seats.setVgap(5);
        HBox legend = new HBox(18,
                legendItem("Ghế trống", "legend-free"),
                legendItem("Ghế đang chọn", "legend-selected"),
                legendItem("Ghế đã bán", "legend-sold"));
        legend.setAlignment(Pos.CENTER);
        getChildren().addAll(screen, seats, legend);
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

    private HBox legendItem(String label, String style) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("legend-swatch", style);
        HBox item = new HBox(5, swatch, new Label(label));
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }
}

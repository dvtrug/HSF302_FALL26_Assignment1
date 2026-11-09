package com.fu.cinema.desktop.component;

import com.fu.cinema.desktop.model.SeatMapData;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class SeatMapView extends VBox {

    private static final PseudoClass SOLD = PseudoClass.getPseudoClass("sold");
    private final GridPane seats = new GridPane();

    public SeatMapView() {
        super(12);
        setAlignment(Pos.TOP_CENTER);
        Label screen = new Label("MÀN HÌNH");
        screen.getStyleClass().add("screen");
        screen.setMaxWidth(Double.MAX_VALUE);
        seats.setAlignment(Pos.CENTER);
        seats.setHgap(5);
        seats.setVgap(5);
        getChildren().addAll(screen, seats);
    }
    public void setData(SeatMapData data) {
        seats.getChildren().clear();
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
        return seat;
    }
}

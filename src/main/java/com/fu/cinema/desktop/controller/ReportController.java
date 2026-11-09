package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class ReportController {

    @FXML private BarChart<String, Number> revenueChart;

    private void renderRevenue(List<RevenueRow> rows) {
        revenueChart.getData().clear();
        if (rows.isEmpty()) {
            return;
        }
        var series = new XYChart.Series<String, Number>();
        int index = 1;
        for (RevenueRow row : rows) {
            // Prefix the category so different movies with the same title have separate bars.
            var bar = new XYChart.Data<String, Number>(index++ + ". " + row.movieTitle(), row.revenue());
            bar.nodeProperty().addListener((observable, previous, node) -> {
                if (node != null) {
                    Tooltip.install(node, new Tooltip(row.movieTitle() + "\n" + row.ticketCount()
                            + " vé\n" + MoneyUtil.format(row.revenue())));
                }
            });
            series.getData().add(bar);
        }
        revenueChart.getData().add(series);
    }
}

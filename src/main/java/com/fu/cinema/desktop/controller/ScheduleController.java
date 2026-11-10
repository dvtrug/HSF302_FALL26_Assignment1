package com.fu.cinema.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ScheduleController {

    @FXML
    private VBox root;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab tabRooms;
}

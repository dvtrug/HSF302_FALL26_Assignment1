package com.fu.cinema.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class RoomController {

    @FXML
    private HBox root;
}

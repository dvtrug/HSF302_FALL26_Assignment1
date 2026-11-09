package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MainController {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");

    private final SceneManager sceneManager;
    private final DesktopSession session;

    @FXML
    private BorderPane root;

    @FXML
    private Label lblUser;

    @FXML
    private Label lblClock;

    private Timeline clock;

    @FXML
    private void initialize() {
        lblUser.textProperty().bind(Bindings.concat("Nhân viên: ", session.displayNameProperty()));
        clock = new Timeline(new KeyFrame(Duration.seconds(1),
                e -> lblClock.setText(LocalDateTime.now().format(CLOCK))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        lblClock.setText(LocalDateTime.now().format(CLOCK));
        onDashboard();
    }

    @FXML
    private void onDashboard() {
        trySwitch(View.DASHBOARD);
    }

    @FXML
    private void onCounterSale() {
        trySwitch(View.COUNTER_SALE);
    }

    @FXML
    private void onBooking() {
        trySwitch(View.BOOKING);
    }

    @FXML
    private void onGenre() {
        trySwitch(View.GENRE);
    }

    @FXML
    private void onMovie() {
        trySwitch(View.MOVIE);
    }

    @FXML
    private void onSchedule() {
        trySwitch(View.SCHEDULE);
    }

    @FXML
    private void onReport() {
        trySwitch(View.REPORT);
    }

    private void trySwitch(View view) {
        try {
            sceneManager.showIn(root, view);
        } catch (Exception e) {
            Label placeholder = new Label(view.title() + " (Đang phát triển)");
            placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #6b7280; -fx-padding: 40;");
            root.setCenter(placeholder);
        }
    }

    @FXML
    private void onLogout() {
        if (clock != null) {
            clock.stop();
        }
        session.logout();
        try {
            sceneManager.switchTo(View.LOGIN);
        } catch (Exception e) {
            // Khi chưa có login.fxml
        }
    }
}

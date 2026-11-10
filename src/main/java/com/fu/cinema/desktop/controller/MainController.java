package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");

    private final SceneManager sceneManager;
    private final DesktopSession session;

    @FXML
    private BorderPane root;

    @FXML
    private Label lblUser;

    @FXML
    private Label lblClock;

    @FXML
    private Button btnDashboard;

    private Timeline clock;
    private Button activeButton;

    @FXML
    private void initialize() {
        lblUser.textProperty().bind(Bindings.concat("Nhân viên: ", session.displayNameProperty()));
        clock = new Timeline(new KeyFrame(Duration.seconds(1),
                e -> lblClock.setText(LocalDateTime.now().format(CLOCK))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        lblClock.setText(LocalDateTime.now().format(CLOCK));
        showView(View.DASHBOARD, btnDashboard);
    }

    @FXML
    private void onDashboard(ActionEvent event) {
        showView(View.DASHBOARD, (Button) event.getSource());
    }

    @FXML
    private void onCounterSale(ActionEvent event) {
        showView(View.COUNTER_SALE, (Button) event.getSource());
    }

    @FXML
    private void onBooking(ActionEvent event) {
        showView(View.BOOKING, (Button) event.getSource());
    }

    @FXML
    private void onGenre(ActionEvent event) {
        showView(View.GENRE, (Button) event.getSource());
    }

    @FXML
    private void onMovie(ActionEvent event) {
        showView(View.MOVIE, (Button) event.getSource());
    }

    @FXML
    private void onSchedule(ActionEvent event) {
        showView(View.SCHEDULE, (Button) event.getSource());
    }

    @FXML
    private void onReport(ActionEvent event) {
        showView(View.REPORT, (Button) event.getSource());
    }

    private void showView(View view, Button button) {
        try {
            sceneManager.showIn(root, view);
        } catch (Exception e) {
            Label placeholder = new Label(view.title() + " (Đang phát triển)");
            placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #6b7280; -fx-padding: 40;");
            root.setCenter(placeholder);
        }
        if (activeButton != null) {
            activeButton.pseudoClassStateChanged(ACTIVE, false);
        }
        button.pseudoClassStateChanged(ACTIVE, true);
        activeButton = button;
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

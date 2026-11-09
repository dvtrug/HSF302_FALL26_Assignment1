package com.fu.cinema.desktop;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe sự kiện StageReadyEvent để khởi tạo giao diện ứng dụng.
 * Ở TODO 33: khởi tạo placeholder scene; TODO 34 sẽ nạp qua SceneManager.
 */
@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        stage.setTitle("FU Cinema — Desktop");
        stage.setScene(new Scene(new StackPane(new Label("FU Cinema Desktop — Quầy vé")), 800, 600));
        stage.show();
    }
}

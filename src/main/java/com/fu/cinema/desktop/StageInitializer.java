package com.fu.cinema.desktop;

import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe sự kiện StageReadyEvent để khởi tạo cửa sổ và chuyển đến màn hình Đăng nhập.
 */
@Component
@RequiredArgsConstructor
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final SceneManager sceneManager;

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        sceneManager.init(event.getStage());
        sceneManager.switchTo(View.LOGIN);
    }
}

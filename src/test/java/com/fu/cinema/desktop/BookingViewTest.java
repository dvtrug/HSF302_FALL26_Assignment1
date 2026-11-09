package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.BookingFacade;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.enums.Role;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledOnOs(OS.WINDOWS)
class BookingViewTest {
    @Autowired ApplicationContext context;
    @Autowired DesktopSession session;
    @Autowired BookingFacade facade;

    @Test void rendersRealDataAndBindsActionsBySelectedStatus() throws Exception {
        session.login(SessionUser.builder().accountId(1L).role(Role.ADMIN).displayName("Test Admin").build());
        Stage stage = FxTestSupport.onFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop/fxml/booking.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load(); Stage window = new Stage();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/desktop/css/desktop.css").toExternalForm());
            window.setScene(scene); window.setTitle("FU Cinema — Quản lý đơn (TODO 44)"); window.show(); return window;
        });
        try {
            TableView<BookingRow> table = FxTestSupport.onFx(() -> (TableView<BookingRow>) stage.getScene().lookup("#table"));
            waitUntil(() -> FxTestSupport.onFx(() -> !table.getItems().isEmpty() && !table.isDisabled()));
            // Exercise the same double-click event as the user, including the asynchronous detail load.
            FxTestSupport.onFx(() -> {
                TableRow<?> row = table.lookupAll(".table-row-cell").stream().filter(n -> n instanceof TableRow<?> r && !r.isEmpty())
                        .map(n -> (TableRow<?>)n).findFirst().orElseThrow();
                row.fireEvent(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                        0,0,0,0,javafx.scene.input.MouseButton.PRIMARY,2,false,false,false,false,false,false,false,false,false,false,null));
                return null;
            });
            waitUntil(() -> FxTestSupport.onFx(() -> javafx.stage.Window.getWindows().stream()
                    .anyMatch(w -> w instanceof Stage s && s.getTitle().startsWith("Chi tiết đơn"))));
            FxTestSupport.onFx(() -> {
                Stage dialog=(Stage)javafx.stage.Window.getWindows().stream().filter(w -> w instanceof Stage s && s.getTitle().startsWith("Chi tiết đơn")).findFirst().orElseThrow();
                TextArea text=(TextArea)dialog.getScene().lookup(".text-area");
                assertTrue(text.getText().contains("Ghế:")); assertTrue(text.getText().contains("Tổng tiền:"));
                capture(dialog.getScene().getRoot(),"05-detail-modal.png"); dialog.close(); return null;
            });
            Long selectedId = FxTestSupport.onFx(() -> table.getItems().stream().filter(r -> r.status()==BookingStatus.PENDING).findFirst().orElseThrow().id());
            FxTestSupport.onFx(() -> {
                var root=stage.getScene().getRoot();
                Button confirm=(Button)root.lookup("#btnConfirm"), cancel=(Button)root.lookup("#btnCancel");
                assertTrue(confirm.isDisabled()); assertTrue(cancel.isDisabled());
                table.getSelectionModel().select(table.getItems().stream().filter(r -> r.id().equals(selectedId)).findFirst().orElseThrow());
                assertFalse(confirm.isDisabled()); assertFalse(cancel.isDisabled());
                capture(root,"01-pending-actions.png");
                return null;
            });
            facade.confirm(selectedId);
            refresh(stage, table);
            FxTestSupport.onFx(() -> {
                table.getSelectionModel().select(table.getItems().stream().filter(r -> r.id().equals(selectedId)).findFirst().orElseThrow());
                assertEquals(BookingStatus.CONFIRMED,table.getSelectionModel().getSelectedItem().status());
                assertTrue(((Button)stage.getScene().lookup("#btnConfirm")).isDisabled());
                assertFalse(((Button)stage.getScene().lookup("#btnCancel")).isDisabled());
                capture(stage.getScene().getRoot(),"02-confirmed-actions.png"); return null;
            });
            facade.cancel(selectedId);
            refresh(stage, table);
            FxTestSupport.onFx(() -> {
                table.getSelectionModel().select(table.getItems().stream().filter(r -> r.id().equals(selectedId)).findFirst().orElseThrow());
                assertEquals(BookingStatus.CANCELLED,table.getSelectionModel().getSelectedItem().status());
                assertTrue(((Button)stage.getScene().lookup("#btnConfirm")).isDisabled());
                assertTrue(((Button)stage.getScene().lookup("#btnCancel")).isDisabled());
                capture(stage.getScene().getRoot(),"03-cancelled-disabled.png"); return null;
            });
            ComboBox<BookingStatus> status = FxTestSupport.onFx(() -> (ComboBox<BookingStatus>)stage.getScene().lookup("#cbStatus"));
            FxTestSupport.onFx(() -> { status.setValue(BookingStatus.PENDING); return null; });
            waitUntil(() -> FxTestSupport.onFx(() -> !table.isDisabled()));
            assertTrue(FxTestSupport.onFx(() -> table.getItems().stream().allMatch(b -> b.status()==BookingStatus.PENDING)));
            FxTestSupport.onFx(() -> { capture(stage.getScene().getRoot(),"04-filter-pending.png"); return null; });
        } finally { FxTestSupport.onFx(() -> { stage.close(); return null; }); }
    }

    private void refresh(Stage stage, TableView<BookingRow> table) throws Exception {
        FxTestSupport.onFx(() -> { ((Button)((javafx.scene.layout.FlowPane)stage.getScene().getRoot().lookup("FlowPane")).getChildren().getLast()).fire(); return null; });
        waitUntil(() -> FxTestSupport.onFx(() -> !table.isDisabled()));
    }
    private void waitUntil(java.util.concurrent.Callable<Boolean> ready) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while(!ready.call()) { if(System.nanoTime()>deadline)fail("UI did not finish loading"); Thread.sleep(50); }
    }
    /** Optional capture of the actual rendered FXML, never a hand-drawn mockup. */
    private void capture(Parent root, String name) throws Exception {
        String folder=System.getProperty("fu.evidence.dir"); if(folder==null)return;
        root.applyCss(); root.layout();
        var image=root.snapshot(null,null);
        java.awt.image.BufferedImage png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var pixels=image.getPixelReader();
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,pixels.getArgb(x,y));
        Path file=Path.of(folder,name); Files.createDirectories(file.getParent()); javax.imageio.ImageIO.write(png,"png",file.toFile());
    }
}

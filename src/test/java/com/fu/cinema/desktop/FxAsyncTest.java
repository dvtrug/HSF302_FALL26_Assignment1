package com.fu.cinema.desktop;

import com.fu.cinema.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.condition.EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
class FxAsyncTest {
    @Test void workIsVirtualAndSuccessRunsOnFxThread() throws Exception {
        FxAsync async = new FxAsync();
        try {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            FxTestSupport.onFx(() -> async.run(() -> Thread.currentThread().isVirtual(),
                    value -> result.complete(value && Platform.isFxApplicationThread()), result::completeExceptionally, null));
            assertTrue(result.get(10, TimeUnit.SECONDS));
        } finally { async.destroy(); }
    }
    @Test void detachingViewCancelsPendingTaskAndSuppressesCallbacks() throws Exception {
        FxAsync async = new FxAsync();
        try {
            AtomicBoolean callback = new AtomicBoolean(); CountDownLatch entered = new CountDownLatch(1);
            StackPane root = FxTestSupport.onFx(StackPane::new);
            Scene scene = FxTestSupport.onFx(() -> new Scene(root));
            Task<?> task = FxTestSupport.onFx(() -> async.forView(root).run(() -> {
                entered.countDown(); new CountDownLatch(1).await(); return "late";
            }, value -> callback.set(true), ex -> callback.set(true)));
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            FxTestSupport.onFx(() -> { scene.setRoot(new StackPane()); return null; });
            assertTrue(task.isCancelled()); assertFalse(callback.get());
        } finally { async.destroy(); }
    }
    @Test void cancellingTaskReleasesBusyNode() throws Exception {
        FxAsync async = new FxAsync();
        try {
            CountDownLatch entered = new CountDownLatch(1);
            StackPane busy = FxTestSupport.onFx(StackPane::new);
            Task<?> task = FxTestSupport.onFx(() -> async.run(() -> {
                entered.countDown(); new CountDownLatch(1).await(); return null;
            }, value -> { }, ex -> { }, busy));
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            FxTestSupport.onFx(() -> { task.cancel(true); return null; });
            assertFalse(FxTestSupport.onFx(busy::isDisabled));
        } finally { async.destroy(); }
    }
}

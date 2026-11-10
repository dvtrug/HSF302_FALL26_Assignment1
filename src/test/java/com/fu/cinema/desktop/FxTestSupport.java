package com.fu.cinema.desktop;

import javafx.application.Platform;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Tests of controls/tasks need a desktop session; pure facade tests do not. */
public final class FxTestSupport {
    private static boolean started;
    private FxTestSupport() { }
    public static synchronized void start() throws Exception {
        if (!started) {
            FutureTask<Void> ready = new FutureTask<>(() -> { Platform.setImplicitExit(false); return null; });
            try { Platform.startup(ready); } catch (IllegalStateException alreadyStarted) { Platform.runLater(ready); }
            ready.get(15, TimeUnit.SECONDS);
            started = true;
        }
    }
    public static <T> T onFx(Callable<T> work) throws Exception {
        start();
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}

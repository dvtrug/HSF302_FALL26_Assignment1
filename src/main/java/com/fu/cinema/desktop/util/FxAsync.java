package com.fu.cinema.desktop.util;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.beans.value.ChangeListener;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.Scene;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class FxAsync implements DisposableBean {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess) {
        return run(work, onSuccess, AlertUtil::showError, null);
    }

    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess, Node busy) {
        return run(work, onSuccess, AlertUtil::showError, busy);
    }

    /**
     * @param busy node bị khoá trong lúc chạy (form, nút...), có thể null
     */
    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError, Node busy) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call(); // background Virtual Thread - KHÔNG đụng UI
            }
        };
        if (busy != null) {
            busy.setDisable(true);
        }
        task.setOnSucceeded(e -> { // JavaFX Application Thread
            if (busy != null) {
                busy.setDisable(false);
            }
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> { // JavaFX Application Thread
            if (busy != null) {
                busy.setDisable(false);
            }
            onError.accept(task.getException());
        });
        task.setOnCancelled(e -> {
            if (busy != null) busy.setDisable(false);
        });
        executor.submit(task);
        return task;
    }

    /** Cho thao tác không trả kết quả (xoá, xác nhận, huỷ...). */
    public Task<Void> runVoid(Runnable work, Runnable onSuccess, Node busy) {
        return run(() -> {
            work.run();
            return null;
        }, v -> onSuccess.run(), AlertUtil::showError, busy);
    }

    /** Create one group per controller. Detaching its root cancels pending UI work. */
    public TaskGroup forView(Node root) {
        return new TaskGroup(root);
    }

    public final class TaskGroup implements AutoCloseable {
        private final Node root;
        private final Set<Task<?>> tasks = new HashSet<>();
        private final ChangeListener<Scene> detached = (p, old, current) -> {
            if (old != null && current == null) close();
        };
        private boolean closed;

        private TaskGroup(Node root) {
            this.root = root;
            root.sceneProperty().addListener(detached);
        }

        /** Start on the FX thread; work runs on a virtual thread, callbacks on the FX thread. */
        public <T> Task<T> run(Callable<T> work, Consumer<T> success, Consumer<Throwable> error) {
            if (closed) throw new IllegalStateException("Màn hình đã đóng");
            Task<T> task = FxAsync.this.run(work,
                    value -> { if (!closed) success.accept(value); },
                    ex -> { if (!closed) error.accept(ex); }, null);
            tasks.add(task);
            task.stateProperty().addListener((p, old, current) -> {
                switch (current) {
                    case SUCCEEDED, FAILED, CANCELLED -> tasks.remove(task);
                    default -> { }
                }
            });
            return task;
        }

        public <T> Task<T> run(Callable<T> work, Consumer<T> success) {
            return run(work, success, AlertUtil::showError);
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            root.sceneProperty().removeListener(detached);
            Set.copyOf(tasks).forEach(task -> task.cancel(true));
            tasks.clear();
        }
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}

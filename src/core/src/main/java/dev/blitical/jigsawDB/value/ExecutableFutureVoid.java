package dev.blitical.jigsawDB.value;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.value.util.RunnableWithException;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ExecutableFutureVoid {
    private final ExecutableFuture<Void> executableFuture;

    public ExecutableFutureVoid(ConnectedDatabase.Exposed exposed, RunnableWithException executable) {
        this.executableFuture = new ExecutableFuture<>(
                exposed,
                () -> {
                    executable.run();
                    return null;
                });
    }

    public synchronized void queue(Runnable success, Consumer<Throwable> failure) {
        this.executableFuture.queue(_ -> success.run(), failure);
    }

    public void complete(boolean shouldQueue) {
        this.executableFuture.complete(shouldQueue);
    }

    public void complete() {
        this.complete(true);
    }

    public void queue() {
        this.queue(() -> {
        }, (e) -> JigsawDBLogger.severe(e, "Error whilst executing: "));
    }

    public void queue(Runnable success) {
        this.queue(success, (e) -> JigsawDBLogger.severe(e, "Error whilst executing: "));
    }

    @CheckReturnValue
    public ExecutableFutureVoid setTimeout(@Range(
            from = 0L,
            to = Long.MAX_VALUE
    ) long timeout, @NotNull TimeUnit unit) {
        this.executableFuture.setTimeout(timeout, unit);
        return this;
    }

    @CheckReturnValue
    public ExecutableFutureVoid setNoTimeout() {
        this.executableFuture.setNoTimeout();
        return this;
    }

    @CheckReturnValue
    public ExecutableFutureVoid setMetadataGetter(ExecutableMetadataGetter getter) {
        this.executableFuture.setMetadataGetter(getter);
        return this;
    }
}

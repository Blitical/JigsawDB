package dev.blitical.jigsawDB.value;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.config.JigsawDBConfig;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExecutableFuture<T> {
    protected final QueueManager queue;
    protected final Supplier<T> executable;
    protected @NotNull Consumer<@NotNull T> success = _ -> {
    };
    protected @NotNull Consumer<Throwable> failure =
            e -> JigsawDBLogger.severe(e, "Error whilst executing: ");
    protected ExecutableMetadataGetter metadataGetter = null;
    protected Long timeout;
    protected TimeUnit timeoutUnit;

    public ExecutableFuture(ConnectedDatabase.Exposed exposed, Supplier<T> executable) {
        this.executable = executable;
        timeout = JigsawDBConfig.ExecutableFuture.TIMEOUT_CONFIG.duration();
        timeoutUnit = JigsawDBConfig.ExecutableFuture.TIMEOUT_CONFIG.unit();
        queue = exposed.queueManager().get();
    }

    public synchronized void queue(@NotNull Consumer<@NotNull T> success, @NotNull Consumer<Throwable> failure) {
        this.success = success;
        this.failure = failure;
        queue.queue(this);
    }

    public @NotNull T complete(boolean shouldQueue) {
        return shouldQueue ? queue.safeQueue(this) : executable.get();
    }

    public @NotNull T complete() {
        return complete(true);
    }

    public void queue() {
        queue.queue(this);
    }

    public void queue(@NotNull Consumer<@NotNull T> success) {
        this.success = success;
        queue.queue(this);
    }

    @CheckReturnValue
    public ExecutableFuture<T> setTimeout(@Range(
            from = 0L,
            to = Long.MAX_VALUE
    ) long timeout, @NotNull TimeUnit unit) {
        this.timeout = timeout;
        this.timeoutUnit = unit;
        return this;
    }

    @CheckReturnValue
    public ExecutableFuture<T> setNoTimeout() {
        this.timeout = null;
        this.timeoutUnit = null;
        return this;
    }

    @CheckReturnValue
    public ExecutableFuture<T> setMetadataGetter(ExecutableMetadataGetter getter) {
        this.metadataGetter = getter;
        return this;
    }
}

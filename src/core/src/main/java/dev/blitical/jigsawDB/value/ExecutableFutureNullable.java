package dev.blitical.jigsawDB.value;

import dev.blitical.jigsawDB.ConnectedDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExecutableFutureNullable<T> extends ExecutableFuture<T> {
    public ExecutableFutureNullable(ConnectedDatabase.Exposed exposed, Supplier<T> executable) {
        super(exposed, executable);
    }

    public synchronized void queue(
            @NotNull Consumer<@Nullable T> success,
            @NotNull Consumer<Throwable> failure
    ) {
        this.success = success;
        this.failure = failure;
        queue.queue(this);
    }

    public @Nullable T complete(boolean shouldQueue) {
        return shouldQueue ? queue.safeQueue(executable) : executable.get();
    }

    public @Nullable T complete() {
        return complete(true);
    }

    public void queue(@NotNull Consumer<@Nullable T> success) {
        this.success = success;
        queue.queue(this);
    }
}

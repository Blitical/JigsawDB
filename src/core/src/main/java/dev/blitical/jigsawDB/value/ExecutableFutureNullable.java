package dev.blitical.jigsawDB.value;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.value.util.SupplierWithException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ExecutableFutureNullable<T> extends ExecutableFuture<T> {
    public ExecutableFutureNullable(ConnectedDatabase.Exposed exposed, SupplierWithException<T> executable) {
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
        try {
            return shouldQueue ? queue.safeQueue(this) : executable.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable T complete() {
        return complete(true);
    }

    public void queue(@NotNull Consumer<@Nullable T> success) {
        this.success = success;
        queue.queue(this);
    }
}

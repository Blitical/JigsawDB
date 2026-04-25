package dev.blitical.jigsawDB.value;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.exceptions.runtime.DatabaseDisconnectedException;
import dev.blitical.jigsawDB.exceptions.runtime.ExecutableFutureTimeoutException;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class QueueManager {
    private static final ThreadLocal<Boolean> IN_QUEUE = ThreadLocal.withInitial(() -> false);
    private final BlockingQueue<QueuedTask<?>> queue = new LinkedBlockingQueue();
    private final ExecutorService executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Thread worker;
    private final ConnectedDatabase.Exposed exposed;

    public void awaitShutdown() {
        this.shutdown.set(true);
        this.worker.interrupt();

        try {
            this.worker.join();
        } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
        }

        this.executor.shutdownNow();
    }

    public QueueManager(ConnectedDatabase.Exposed exposed) {
        this.exposed = exposed;
        this.executor = Executors.newSingleThreadExecutor((r) -> new Thread(r, "ExecutableFutureQueuedTask-" + exposed.driver().formatedName()));
        this.worker = new Thread(this::run, "ExecutableFutureQueue-" + exposed.driver().formatedName());
        this.worker.start();
    }

    private void run() {
        while (!this.shutdown.get() || !this.queue.isEmpty()) {
            QueuedTask<?> task;
            try {
                task = this.queue.take();
            } catch (InterruptedException e) {
                if (!this.shutdown.get() || !this.queue.isEmpty())
                    continue;
                break;
            }

            task.execute();
        }

    }

    public synchronized <T> QueuedTask<T> queue(ExecutableFuture<T> future) {
        if (!this.exposed.isOpen() && !this.exposed.driver().driverIsNull()) {
            throw new DatabaseDisconnectedException(this.exposed.driver().formatedName());
        }
        QueuedTask<T> task = new QueuedTask<>(this, future);
        this.queue.add(task);
        return task;
    }

    public <T> T safeQueue(Supplier<T> supplier) {
        if (!this.exposed.isOpen() && !this.exposed.driver().driverIsNull()) {
            throw new DatabaseDisconnectedException(this.exposed.driver().formatedName());
        }
        if (inQueueThread()) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                throw t instanceof RuntimeException ex
                        ? ex : new RuntimeException(t);
            }
        }

        return this.queue(new ExecutableFuture<>(this.exposed, supplier)).await();
    }

    public static class QueuedTask<T> {
        private final ExecutableFuture<T> f;
        private final CountDownLatch latch = new CountDownLatch(1);

        protected T result;
        protected Throwable error;
        protected Long start;
        protected Long end;
        private final QueueManager manager;
        private volatile boolean timedOut = false;

        private QueuedTask(
                QueueManager manager,
                ExecutableFuture<T> future
        ) {
            this.manager = manager;
            this.f = future;
        }

        private void execute() {
            Future<T> future = manager.executor.submit(() -> {
                IN_QUEUE.set(true);
                try {
                    start = System.nanoTime();
                    return f.executable.get();
                } finally {
                    end = System.nanoTime();
                    IN_QUEUE.set(false);
                }
            });

            try {
                result = future.get(f.timeout, f.timeoutUnit);
                f.success.accept(result);
            } catch (TimeoutException e) {
                future.cancel(true);
                timedOut = true;
                error = new ExecutableFutureTimeoutException(f.timeout, f.timeoutUnit);
                f.failure.accept(error);
            } catch (Throwable t) {
                error = t;
                f.failure.accept(t);
            } finally {
                if (f.metadataGetter != null) {
                    f.metadataGetter.setResult(
                            new ExecutableMetadataGetter.Result(start, end, end - start, timedOut)
                    );
                }
                latch.countDown();
            }
        }

        public T await() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            if (error != null) {
                throw error instanceof RuntimeException ex
                        ? ex : new RuntimeException(error);
            }
            return result;
        }
    }

    public static boolean inQueueThread() {
        return Boolean.TRUE.equals(IN_QUEUE.get());
    }
}

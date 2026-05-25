package dev.blitical.jigsawDB.value;

import java.util.concurrent.CountDownLatch;

public class ExecutableMetadataGetter {
    private final CountDownLatch latch = new CountDownLatch(1);
    private Result result = null;

    public record Result(
            Long start,
            Long end,
            Long duration,
            boolean timedOut
    ) {
    }

    protected void setResult(Result result) {
        this.result = result;
        latch.countDown();
    }

    public Result awaitResult() throws InterruptedException {
        latch.await();
        return result;
    }

    public Result awaitResultWithoutException() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public Result getResult() {
        return result;
    }

    public boolean resultExists() {
        return result != null;
    }
}

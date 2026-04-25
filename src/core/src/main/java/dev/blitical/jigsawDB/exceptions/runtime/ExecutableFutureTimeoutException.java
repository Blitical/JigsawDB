package dev.blitical.jigsawDB.exceptions.runtime;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

import java.util.concurrent.TimeUnit;

@JigsawDBException(
        severity = JigsawDBException.Severity.MEDIUM
)
public class ExecutableFutureTimeoutException extends DatabaseRuntimeException {
    public ExecutableFutureTimeoutException(Long timeout, TimeUnit unit) {
        super("Queued task exceeded timeout of " + timeout + " " + unit.toString());
    }
}

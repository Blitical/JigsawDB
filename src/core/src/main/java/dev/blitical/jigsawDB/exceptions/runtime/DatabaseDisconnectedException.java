package dev.blitical.jigsawDB.exceptions.runtime;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.HIGH,
        fixes = {
                "Ensuring that you never call a disconnected database"
        }
)
public class DatabaseDisconnectedException extends DatabaseRuntimeException {
    public DatabaseDisconnectedException(String databaseName) {
        super(String.format("Attempting to modify an already disconnected database (%s)", databaseName));
    }
}

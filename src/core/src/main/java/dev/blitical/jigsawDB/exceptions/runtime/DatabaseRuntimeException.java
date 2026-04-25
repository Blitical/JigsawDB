package dev.blitical.jigsawDB.exceptions.runtime;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.DatabaseException;

public class DatabaseRuntimeException extends DatabaseException {
    public DatabaseRuntimeException(String message) {
        super("Database Runtime Exception", message);
    }
}

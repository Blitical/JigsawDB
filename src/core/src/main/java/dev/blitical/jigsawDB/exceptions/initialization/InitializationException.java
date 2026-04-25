package dev.blitical.jigsawDB.exceptions.initialization;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.DatabaseException;

public class InitializationException extends DatabaseException {
    public InitializationException(String message) {
        super("Database Initialisation Exception", message);
    }
}

package dev.blitical.jigsawDB.exceptions.compile;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.DatabaseException;

public class CompileException extends DatabaseException {
    public CompileException(String message) {
        super("Database Compile Exception", message);
    }
}

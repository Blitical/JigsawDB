package dev.blitical.jigsawDB.exceptions.encoder;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.DatabaseException;

public class EncodingException extends DatabaseException {
    public EncodingException(String msg) {
        super("Encode Exception", msg);
    }
}

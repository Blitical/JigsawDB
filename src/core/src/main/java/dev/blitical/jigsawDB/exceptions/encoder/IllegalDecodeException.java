package dev.blitical.jigsawDB.exceptions.encoder;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE
)
public class IllegalDecodeException extends EncodingException {
    public IllegalDecodeException(String columnName, String attemptedType) {
        super(String.format("Failed to decode '%s' to %s", columnName, attemptedType));
    }
}

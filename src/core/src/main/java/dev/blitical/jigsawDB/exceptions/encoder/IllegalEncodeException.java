package dev.blitical.jigsawDB.exceptions.encoder;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE
)
public class IllegalEncodeException extends EncodingException {
    public IllegalEncodeException(String value, String type) {
        super(String.format("Failed to encode '%s' to %s", value, type));
    }
}

package dev.blitical.jigsawDB.exceptions.encoder;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE
)
public class IllegalParseTypeException extends EncodingException {
    public IllegalParseTypeException(String msg) {
        super(msg);
    }
}

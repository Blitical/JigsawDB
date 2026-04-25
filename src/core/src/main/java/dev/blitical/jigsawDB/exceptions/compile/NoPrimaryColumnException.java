package dev.blitical.jigsawDB.exceptions.compile;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE,
        fixes = {
                "Adding a primary column annotated with @PrimaryColumn"
        }
)
public class NoPrimaryColumnException extends CompileException {
    public NoPrimaryColumnException(String tableName) {
        super(String.format("No primary column found in table '%s'", tableName));
    }
}

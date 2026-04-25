package dev.blitical.jigsawDB.exceptions.initialization;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE,
        fixes = {
                "Renaming the table to a different class name",
                "Overriding the table name in the table config"
        }
)
public class DuplicateTableException extends InitializationException {
    public DuplicateTableException(String tableName) {
        super("Duplicate table names have been found: " + tableName);
    }

    public DuplicateTableException() {
        super("Duplicate table names have been found");
    }
}

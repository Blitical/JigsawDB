package dev.blitical.jigsawDB.exceptions.initialization;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE,
        fixes = {
                "Renaming the table to a different class name",
                "Overriding the table name in the table config",
                "Ensuring that you do not have the same table registered as a RegularTable and a ShadowTable"
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

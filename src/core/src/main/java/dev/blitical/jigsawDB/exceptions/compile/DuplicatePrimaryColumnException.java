package dev.blitical.jigsawDB.exceptions.compile;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE,
        fixes = {
                "Making sure that there's only 1 primary column per table"
        }
)
public class DuplicatePrimaryColumnException extends CompileException {
    public DuplicatePrimaryColumnException(String tableName, String primaryColumn1, String primaryColumn2) {
        super(String.format(
                "Duplicate primary columns in table '%s' names have been found: '%s' and '%s'",
                tableName, primaryColumn1, primaryColumn2
        ));
    }

    public DuplicatePrimaryColumnException(String tableName) {
        super(String.format(
                "Duplicate primary columns in table '%s'",
                tableName
        ));
    }
}

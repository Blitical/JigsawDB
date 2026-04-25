package dev.blitical.jigsawDB.exceptions.compile;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE,
        fixes = {
                "Ensuring both the primary column and table type are the same.",
        },
        correct = """
                for `myTable extends Table<MyTable, UUID>`,
                    the field annotated with '@PrimaryKey' MUST be of type UUID.
                """
)
public class TableAndPrimaryColumnMismatch extends CompileException {
    public TableAndPrimaryColumnMismatch(String tableName, String expected, String got) {
        super(String.format(
                "For '%s', expected primary key to be of type '%s'. Got '%s'",
                tableName, expected, got
        ));
    }
}

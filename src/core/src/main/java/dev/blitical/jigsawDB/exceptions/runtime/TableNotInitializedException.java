package dev.blitical.jigsawDB.exceptions.runtime;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;
import dev.blitical.jigsawDB.table.Table;

@JigsawDBException(
        severity = JigsawDBException.Severity.HIGH,
        fixes = {
                "Ensuring you initialized your table in the builder"
        },
        correct = """
                new DatabaseBuilder(...)
                        .addTable(YOUR TABLE HERE)
                                  ^^^^^^^^^^^^^^^
                        .connect().complete();"""
)
public class TableNotInitializedException extends DatabaseRuntimeException {
    public TableNotInitializedException(Class<Table<?, ?>> tableClass) {
        super(String.format("The table '%s' has not been initialized yet", tableClass.getSimpleName()));
    }
}

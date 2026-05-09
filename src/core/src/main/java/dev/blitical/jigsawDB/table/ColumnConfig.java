package dev.blitical.jigsawDB.table;

import dev.blitical.jigsawDB.drivers.types.TypeDefinition;

public class ColumnConfig<T> {
    protected T defaultValue = null;
    protected boolean nullable = true;
    protected boolean unique = false;
    protected boolean autoIncrement = false;
    protected TypeDefinition typeDefinition = null;

    public DefinedColumnConfig<T> asDefinedConfig() {
        return new DefinedColumnConfig<>(
                defaultValue,
                nullable,
                unique,
                autoIncrement,
                typeDefinition
        );
    }
}

package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.table.ColumnConfig;

public class PrimaryGenericColumnConfig<T> extends ColumnConfig<T> {
    public PrimaryGenericColumnConfig() {
        this.defaultValue = null;
        this.unique = true;
        this.autoIncrement = false;
        this.nullable = false;
    }
}

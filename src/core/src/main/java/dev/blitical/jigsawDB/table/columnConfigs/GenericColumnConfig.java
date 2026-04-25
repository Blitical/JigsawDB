package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.table.ColumnConfig;

public class GenericColumnConfig<T> extends ColumnConfig<T> {
    public ColumnConfig<T> setDefault(T value) {
        this.defaultValue = value;
        return this;
    }

    public ColumnConfig<T> notNull(T defaultValue) {
        this.nullable = false;
        this.defaultValue = defaultValue;
        return this;
    }

    public ColumnConfig<T> unique(boolean unique) {
        this.unique = unique;
        return this;
    }
}

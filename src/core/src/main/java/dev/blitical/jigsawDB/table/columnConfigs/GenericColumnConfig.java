package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.GenericTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;

public class GenericColumnConfig<T> extends ColumnConfig<T> {
    public GenericColumnConfig<T> setDefault(T value) {
        this.defaultValue = value;
        return this;
    }

    public GenericColumnConfig<T> notNull(T defaultValue) {
        this.nullable = false;
        this.defaultValue = defaultValue;
        return this;
    }

    public GenericColumnConfig<T> unique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public GenericColumnConfig<T> columnType(GenericTypeDefinition type) {
        this.typeDefinition = type;
        return this;
    }
}

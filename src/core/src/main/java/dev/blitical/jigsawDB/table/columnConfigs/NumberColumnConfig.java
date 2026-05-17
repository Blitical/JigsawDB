package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.NumberTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;

public class NumberColumnConfig<T> extends ColumnConfig<T> {
    public NumberColumnConfig<T> setDefault(T value) {
        this.defaultValue = value;
        return this;
    }

    public NumberColumnConfig<T> notNull(T defaultValue) {
        this.nullable = false;
        this.defaultValue = defaultValue;
        return this;
    }

    public NumberColumnConfig<T> unique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public NumberColumnConfig<T> columnType(NumberTypeDefinition type) {
        this.typeDefinition = type;
        return this;
    }
}

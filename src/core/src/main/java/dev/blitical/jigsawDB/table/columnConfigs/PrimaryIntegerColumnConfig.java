package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.PrimaryIntegerTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;

public class PrimaryIntegerColumnConfig<T> extends ColumnConfig<T> {
    public PrimaryIntegerColumnConfig() {
        this.defaultValue = null;
        this.unique = true;
        this.autoIncrement = false;
        this.nullable = false;
    }

    public ColumnConfig<T> autoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
        return this;
    }

    public ColumnConfig<T> autoIncrement() {
        return autoIncrement(true);
    }

    public ColumnConfig<T> columnType(PrimaryIntegerTypeDefinition type) {
        this.typeDefinition = type;
        return this;
    }
}

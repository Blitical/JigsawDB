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

    public PrimaryIntegerColumnConfig<T> autoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
        return this;
    }

    public PrimaryIntegerColumnConfig<T> autoIncrement() {
        return autoIncrement(true);
    }

    public PrimaryIntegerColumnConfig<T> columnType(PrimaryIntegerTypeDefinition type) {
        this.typeDefinition = type;
        return this;
    }
}

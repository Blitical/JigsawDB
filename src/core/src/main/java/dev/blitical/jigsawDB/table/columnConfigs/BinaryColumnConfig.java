package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.BinaryTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;

public class BinaryColumnConfig<T> extends ColumnConfig<T> {
    public ColumnConfig<T> columnType(BinaryTypeDefinition type) {
        this.typeDefinition = type;
        return this;
    }
}

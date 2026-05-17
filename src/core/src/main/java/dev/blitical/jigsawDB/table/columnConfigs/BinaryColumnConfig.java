package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.BinaryTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class BinaryColumnConfig<T> extends ColumnConfig<T> {

    public BinaryColumnConfig<T> setDefault(@NotNull Supplier<@NotNull T> value) {
        this.defaultSupplier = value;
        this.supplierConstant = false;
        return this;
    }

    public BinaryColumnConfig<T> columnType(BinaryTypeDefinition type) {
        this.typeDefinition = type;
        return this;
    }
}

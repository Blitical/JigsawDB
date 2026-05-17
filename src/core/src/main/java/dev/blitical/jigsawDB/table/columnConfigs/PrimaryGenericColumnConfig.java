package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.PrimaryGenericTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class PrimaryGenericColumnConfig<T> extends ColumnConfig<T> {
    public PrimaryGenericColumnConfig() {
        this.unique = true;
        this.autoIncrement = false;
        this.nullable = false;
    }

    public PrimaryGenericColumnConfig<T> setDefault(@NotNull Supplier<@NotNull T> value) {
        this.defaultSupplier = value;
        this.supplierConstant = false;
        return this;
    }

    public PrimaryGenericColumnConfig<T> columnType(PrimaryGenericTypeDefinition type) {
        this.typeDefinition = type;
        return this;
    }
}

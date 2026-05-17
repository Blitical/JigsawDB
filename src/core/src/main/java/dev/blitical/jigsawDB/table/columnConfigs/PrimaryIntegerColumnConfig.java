package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.PrimaryIntegerTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class PrimaryIntegerColumnConfig<T> extends ColumnConfig<T> {
    public PrimaryIntegerColumnConfig() {
        this.unique = true;
        this.autoIncrement = false;
        this.nullable = false;
    }

    public PrimaryIntegerColumnConfig<T> setDefault(@NotNull Supplier<@NotNull T> value) {
        this.defaultSupplier = value;
        this.supplierConstant = false;
        return this;
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

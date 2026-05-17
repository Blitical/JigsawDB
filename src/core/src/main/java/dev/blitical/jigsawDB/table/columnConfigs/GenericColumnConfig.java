package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.GenericTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class GenericColumnConfig<T> extends ColumnConfig<T> {
    public GenericColumnConfig<T> setDefault(T value) {
        this.defaultSupplier = () -> value;
        this.supplierConstant = true;
        return this;
    }

    public GenericColumnConfig<T> setDefault(@NotNull Supplier<@NotNull T> value) {
        this.defaultSupplier = value;
        this.supplierConstant = false;
        return this;
    }

    public GenericColumnConfig<T> notNull(T defaultValue) {
        this.nullable = false;
        this.defaultSupplier = () -> defaultValue;
        this.supplierConstant = true;
        return this;
    }

    public GenericColumnConfig<T> notNull(@NotNull Supplier<@NotNull T> defaultValue) {
        this.nullable = false;
        this.defaultSupplier = defaultValue;
        this.supplierConstant = false;
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

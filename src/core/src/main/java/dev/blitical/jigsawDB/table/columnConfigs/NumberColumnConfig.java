package dev.blitical.jigsawDB.table.columnConfigs;

import dev.blitical.jigsawDB.drivers.types.definition.NumberTypeDefinition;
import dev.blitical.jigsawDB.table.ColumnConfig;

import java.util.function.Supplier;

public class NumberColumnConfig<T> extends ColumnConfig<T> {
    public NumberColumnConfig<T> setDefault(T value) {
        this.defaultSupplier = () -> value;
        this.supplierConstant = true;
        return this;
    }

    public NumberColumnConfig<T> setDefault(Supplier<T> value) {
        this.defaultSupplier = value;
        this.supplierConstant = false;
        return this;
    }

    public NumberColumnConfig<T> notNull(T defaultValue) {
        this.nullable = false;
        this.defaultSupplier = () -> defaultValue;
        this.supplierConstant = true;
        return this;
    }

    public NumberColumnConfig<T> notNull(Supplier<T> value) {
        this.nullable = false;
        this.defaultSupplier = value;
        this.supplierConstant = false;
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

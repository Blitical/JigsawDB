package dev.blitical.jigsawDB.table;

import dev.blitical.jigsawDB.drivers.types.TypeDefinition;

import java.util.function.Supplier;

public class ColumnConfig<T> {
    protected Supplier<T> defaultSupplier = () -> null;
    protected boolean supplierConstant = false;
    protected boolean nullable = true;
    protected boolean unique = false;
    protected boolean autoIncrement = false;
    protected TypeDefinition typeDefinition = null;

    public DefinedColumnConfig<T> asDefinedConfig() {
        return new DefinedColumnConfig<>(
                defaultSupplier,
                supplierConstant,
                nullable,
                unique,
                autoIncrement,
                typeDefinition
        );
    }
}

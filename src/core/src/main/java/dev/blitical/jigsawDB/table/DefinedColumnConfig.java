package dev.blitical.jigsawDB.table;

import dev.blitical.jigsawDB.drivers.types.TypeDefinition;

import java.util.function.Supplier;

public record DefinedColumnConfig<T>(
        Supplier<T> defaultSupplier,
        boolean supplierConstant,
        boolean nullable,
        boolean unique,
        boolean autoIncrement,
        TypeDefinition typeDefinition
) {
}

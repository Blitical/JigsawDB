package dev.blitical.jigsawDB.table;

import dev.blitical.jigsawDB.drivers.types.TypeDefinition;

public record DefinedColumnConfig<T>(
        T defaultValue,
        boolean nullable,
        boolean unique,
        boolean autoIncrement,
        TypeDefinition typeDefinition
) {
}

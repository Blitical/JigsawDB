package dev.blitical.jigsawDB.table;

public record DefinedColumnConfig<T>(
        T defaultValue,
        boolean nullable,
        boolean unique,
        boolean autoIncrement
) {
}

package dev.blitical.jigsawDB.drivers.misc;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public record PredefinedColumn(
        @NotNull String name,
        Field field,
        Object defaultValue,
        String formatedDefault,
        boolean nullable,
        boolean unique,
        boolean autoIncrement,
        boolean primaryKey
) {
}

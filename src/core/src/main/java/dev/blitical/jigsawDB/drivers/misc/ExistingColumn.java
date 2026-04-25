package dev.blitical.jigsawDB.drivers.misc;

import org.jetbrains.annotations.NotNull;

public record ExistingColumn(
        @NotNull String name,
        @NotNull String type,
        boolean nullable,
        boolean primaryKey
) {
}

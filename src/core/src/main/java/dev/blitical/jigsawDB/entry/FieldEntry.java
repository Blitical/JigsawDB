package dev.blitical.jigsawDB.entry;

import dev.blitical.jigsawDB.table.Table;

public record FieldEntry<T extends Table<T, ?>, F extends Field<T, V>, V>(
        Table<T, ?> table,
        Field<T, V> field,
        V value
) {
}

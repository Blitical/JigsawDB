package dev.blitical.jigsawDB.entry.fields;

import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.TypeToken;
import dev.blitical.jigsawDB.table.Table;

public class BinaryField<E extends Table<E, ?>, T> extends Field<E, T> {
    public BinaryField(String name, TypeToken<T> type) {
        super(name, type);
    }
}

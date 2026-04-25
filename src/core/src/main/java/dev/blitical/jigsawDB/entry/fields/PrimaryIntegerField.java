package dev.blitical.jigsawDB.entry.fields;

import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.TypeToken;
import dev.blitical.jigsawDB.table.Table;

public class PrimaryIntegerField<E extends Table<E, ?>, T> extends Field<E, T> {
    public PrimaryIntegerField(String name, TypeToken<T> type) {
        super(name, type);
    }
}

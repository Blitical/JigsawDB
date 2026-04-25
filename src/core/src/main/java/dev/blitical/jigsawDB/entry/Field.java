package dev.blitical.jigsawDB.entry;

import dev.blitical.jigsawDB.table.Table;

public class Field<E extends Table<E, ?>, T> {
    private final String name;
    private final TypeToken<T> type;

    protected Field(String name, TypeToken<T> type) {
        this.name = name;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public T parse(Object obj) {
        return (T) obj;
    }

    public TypeToken<T> getTypeToken() {
        return type;
    }

    public String name() {
        return name;
    }
}

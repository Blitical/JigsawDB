package dev.blitical.jigsawDB.table;

import java.lang.reflect.Constructor;

public abstract class GeneratedTable<
        S extends GeneratedTable<S, T>,
        T extends Table<T, ?>
        > {

    protected S self;
    private final T table;

    public GeneratedTable(T table) {
        this.table = table;
    }

    public static <T extends Table<T, ?>, S extends GeneratedTable<S, T>>
    GeneratedTable<S, T> newInstance(Table<T, ?> table) {
        try {
            String generatedClassName = table.getClass().getName() + "Fields";
            Class<?> genClass = Class.forName(generatedClassName);
            Constructor<?> ctor = genClass.getDeclaredConstructor(table.getClass());
            ctor.setAccessible(true);
            return (GeneratedTable<S, T>) ctor.newInstance(table);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public S getSelf() {
        return self;
    }

    public T getLinkedTable() {
        return table;
    }
}

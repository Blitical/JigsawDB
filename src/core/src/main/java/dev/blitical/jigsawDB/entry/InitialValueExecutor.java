package dev.blitical.jigsawDB.entry;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class InitialValueExecutor<T extends Table<T, P>, P> {
    private final ConnectedDatabase.Exposed exposed;
    private final Map<String, InitialValue<T, ?>> values = new HashMap<>();

    public record InitialValue<T extends Table<T, ?>, V>(
            Field<T, V> field,
            V value
    ) {
    }

    public record Built<T extends Table<T, P>, P>(
            Map<String, InitialValue<T, ?>> values
    ) {
    }

    public InitialValueExecutor(ConnectedDatabase.Exposed exposed) {
        this.exposed = exposed;
    }

    @CheckReturnValue
    public <V> @NotNull InitialValueExecutor<T, P> set(Field<T, V> field, V value) {
        values.put(field.name(), new InitialValue<>(field, value));
        return this;
    }

    public Built<T, P> build() {
        return new Built<>(Map.copyOf(values));
    }
}

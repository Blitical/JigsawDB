package dev.blitical.jigsawDB.entry;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.drivers.action.Action;
import dev.blitical.jigsawDB.drivers.action.GetAction;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public class BucketGetter<T extends Table<T, P>, P> {
    private final Table<T, P> table;
    private final P primaryKey;
    private final ConnectedDatabase.Exposed exposed;

    public BucketGetter(
            Table<T, P> table,
            P primaryKey,
            ConnectedDatabase.Exposed exposed
    ) {
        this.table = table;
        this.primaryKey = primaryKey;
        this.exposed = exposed;
    }

    @CheckReturnValue
    public <V> @NotNull Action set(Field<T, V> field, V value) {
        return exposed.driver().set(table, primaryKey, List.of(new FieldEntry<>(table, field, value)))
                .onComplete(() -> {
                    if (Entry.CACHE_IF.contains(CacheHandler.getPolicy(exposed, table, field))) {
                        CacheHandler.putCachedValue(exposed, table, primaryKey, field, value);
                    }
                });
    }

    @CheckReturnValue
    public <V> @NotNull Action setWithInputStream(Field<T, V> field, InputStream stream) {
        return exposed.driver().setWithInputStream(table, primaryKey, field, stream);
    }

    @CheckReturnValue
    public <V> @NotNull GetAction<V> get(Field<T, V> field) {
        return exposed.driver().get(table, primaryKey, field).onGet(v -> {
            if (Entry.CACHE_IF.contains(CacheHandler.getPolicy(exposed, table, field))) {
                CacheHandler.putCachedValue(exposed, table, primaryKey, field, v);
            }
        });
    }

    @CheckReturnValue
    public <V> @NotNull GetAction<V> get(Field<T, V> field, Consumer<V> executor) {
        return exposed.driver().get(table, primaryKey, field).onGet(v -> {
            if (Entry.CACHE_IF.contains(CacheHandler.getPolicy(exposed, table, field))) {
                CacheHandler.putCachedValue(exposed, table, primaryKey, field, v);
            }
            executor.accept(v);
        });
    }

    @CheckReturnValue
    public <V> @NotNull GetAction<InputStream> getAsInputStream(Field<T, V> field) {
        return exposed.driver().getAsInputStream(table, primaryKey, field);
    }

    @CheckReturnValue
    public <V> @NotNull GetAction<InputStream> getAsInputStream(Field<T, V> field, Consumer<InputStream> executor) {
        return exposed.driver().getAsInputStream(table, primaryKey, field).onGet(executor);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public @NotNull Action drop() {
        return exposed.driver().dropEntry(table, primaryKey)
                .onComplete(() ->
                    exposed.database().breakEntryFromCache(table.getClass(), primaryKey)
                );
    }
}

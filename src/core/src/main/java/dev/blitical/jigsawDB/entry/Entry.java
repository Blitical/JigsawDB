package dev.blitical.jigsawDB.entry;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.value.ExecutableFutureNullable;
import dev.blitical.jigsawDB.value.ExecutableFutureVoid;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class Entry<T extends Table<T, P>, P> {

    public static final Set<CachePolicy.Policy> cacheIf = Set.of(
            CachePolicy.Policy.LAZY,
            CachePolicy.Policy.EAGER
    );

    private final ConnectedDatabase.Exposed exposed;
    private final Table<T, P> table;
    public final P primaryKey;
    public final boolean exists;

    @SuppressWarnings("unchecked")
    public Entry(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey,
            boolean createIfNotExists
    ) {
        this.exposed = database;
        this.table = table;
        this.primaryKey = primaryKey;

        try {
            if (!checkEntryAndCache()) {
                if (!createIfNotExists) {
                    this.exists = false;
                    return;
                }

                exposed.database().createEntry(table.getClass(), primaryKey).complete();
                checkEntryAndCache();
            }
            this.exists = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Entry(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey
    ) {
        this.exposed = database;
        this.table = table;
        this.primaryKey = primaryKey;
        this.exists = true;
    }

    private boolean checkEntryAndCache() throws SQLException {
        return exposed.driver().checkEntryAndCache(
                table,
                this,
                CacheHandler.getFieldsByPolicy(
                        exposed, table, CachePolicy.Policy.EAGER
                ),
                this::putCachedValue
        );
    }

    @SuppressWarnings("unchecked")
    private <F extends Field<T, V>, V> void putCachedValue(
            Field<T, ?> field,
            Object value
    ) {
        CacheHandler.putCachedValue(exposed, table, primaryKey, (Field<T, V>) field, (V) value);
    }

    @CheckReturnValue
    public <V> @NotNull ExecutableFutureVoid set(Field<T, V> field, V value) {
        return new ExecutableFutureVoid(exposed, () -> {
            try {
                exposed.driver().set(table, primaryKey, List.of(new FieldEntry<>(table, field, value)));
                if (cacheIf.contains(CacheHandler.getPolicy(exposed, table, field))) {
                    CacheHandler.putCachedValue(exposed, table, primaryKey, field, value);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @CheckReturnValue
    public <V> @NotNull ExecutableFutureNullable<V> get(Field<T, V> field) {
        return new ExecutableFutureNullable<>(exposed, () -> {
            var cachedObject = CacheHandler.getCachedValue(exposed, table, primaryKey, field);
            if (cachedObject.isCached()) {
                JigsawDBLogger.debug("CACHED VALUE TYPE: " + cachedObject.value().getClass());
                return cachedObject.value();
            }

            V value;
            try {
                value = exposed.driver().get(table, primaryKey, field);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            if (cacheIf.contains(CacheHandler.getPolicy(exposed, table, field))) {
                CacheHandler.putCachedValue(exposed, table, primaryKey, field, value);
            }

            return value;
        });
    }

    @CheckReturnValue
    public <V> @NotNull ExecutableFutureVoid setWithInputStream(Field<T, V> field, InputStream stream) {
        return new ExecutableFutureVoid(exposed, () -> {
            try {
                exposed.driver().setWithInputStream(table, primaryKey, field, stream);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @CheckReturnValue
    public <V> @NotNull ExecutableFutureNullable<InputStream> getAsInputStream(Field<T, V> field) {
        return new ExecutableFutureNullable<>(exposed, () -> {
            try {
                return exposed.driver().getAsInputStream(table, primaryKey, field);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public @NotNull ExecutableFutureVoid drop() {
        return new ExecutableFutureVoid(exposed, () -> {
            try {
                exposed.driver().dropEntry(table, primaryKey);
                exposed.database().breakEntry(table.getClass(), primaryKey);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

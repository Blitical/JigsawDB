package dev.blitical.jigsawDB.entry;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.value.ExecutableFutureNullable;
import dev.blitical.jigsawDB.value.ExecutableFutureVoid;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Set;
import java.util.function.Function;

public class Entry<T extends Table<T, P>, P> {

    public static final Set<CachePolicy.Policy> CACHE_IF = Set.of(
            CachePolicy.Policy.LAZY,
            CachePolicy.Policy.EAGER
    );

    private final ConnectedDatabase.Exposed exposed;
    private final Table<T, P> table;
    public final P primaryKey;
    public final boolean exists;
    private final BucketGetter<T, P> bucketGetter;

    @SuppressWarnings("unchecked")
    public Entry(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey,
            boolean createIfNotExists,
            Function<InitialValueExecutor<T, P>, InitialValueExecutor.Built<T, P>> initialValues
    ) {
        this.exposed = database;
        this.table = table;
        this.primaryKey = primaryKey;
        this.bucketGetter = new BucketGetter<>(table, primaryKey, exposed);

        try {
            if (!checkEntryAndCache()) {
                if (!createIfNotExists) {
                    this.exists = false;
                    return;
                }

                exposed.database().createEntry(
                        (Class<T>) table.getClass(),
                        primaryKey,
                        initialValues
                ).complete();
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
        this.bucketGetter = new BucketGetter<>(table, primaryKey, exposed);
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
        return new ExecutableFutureVoid(exposed, () ->
            bucketGetter.set(field, value).execute(exposed)
        );
    }

    @CheckReturnValue
    public <V> @NotNull ExecutableFutureNullable<V> get(Field<T, V> field) {
        return new ExecutableFutureNullable<>(exposed, () -> {
            var cachedObject = CacheHandler.getCachedValue(exposed, table, primaryKey, field);
            if (cachedObject.isCached()) {
                return cachedObject.value();
            }
            return bucketGetter.get(field).execute(exposed);
        });
    }

    @CheckReturnValue
    public <V> @NotNull ExecutableFutureVoid setWithInputStream(Field<T, V> field, InputStream stream) {
        return new ExecutableFutureVoid(exposed, () ->
            bucketGetter.setWithInputStream(field, stream).execute(exposed)
        );
    }

    @CheckReturnValue
    public <V> @NotNull ExecutableFutureNullable<InputStream> getAsInputStream(Field<T, V> field) {
        return new ExecutableFutureNullable<>(exposed, () ->
            bucketGetter.getAsInputStream(field).execute(exposed)
        );
    }

    @CheckReturnValue
    public @NotNull ExecutableFutureVoid drop() {
        return new ExecutableFutureVoid(exposed, () ->
                bucketGetter.drop().execute(exposed)
        );
    }

    @CheckReturnValue
    public @NotNull BucketGetter<T, P> bucketGetter() {
        return bucketGetter;
    }

    @CheckReturnValue
    public @NotNull BatchValueExecutor<T, P> batch() {
        return new BatchValueExecutor<>(table, primaryKey, exposed, bucketGetter);
    }
}

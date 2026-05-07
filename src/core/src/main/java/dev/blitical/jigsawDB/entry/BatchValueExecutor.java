package dev.blitical.jigsawDB.entry;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.drivers.action.Bucket;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.value.ExecutableFutureVoid;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.function.Consumer;

public final class BatchValueExecutor<T extends Table<T, P>, P> {
    private final Table<T, P> table;
    private final P primaryKey;
    private final ConnectedDatabase.Exposed exposed;
    private final BucketGetter<T, P> bucketGetter;
    private final Bucket bucket;

    public BatchValueExecutor(
        Table<T, P> table,
        P primaryKey,
        ConnectedDatabase.Exposed exposed,
        BucketGetter<T, P> bucketGetter
    ) {
        this.table = table;
        this.primaryKey = primaryKey;
        this.exposed = exposed;
        this.bucketGetter = bucketGetter;
        this.bucket = exposed.database().createBucket();
    }

    @CheckReturnValue
    public <V> @NotNull BatchValueExecutor<T, P> set(Field<T, V> field, V value) {
        bucket.add(bucketGetter.set(field, value));
        return this;
    }

    @CheckReturnValue
    public <V> @NotNull BatchValueExecutor<T, P> setWithInputStream(Field<T, V> field, InputStream stream) {
        bucket.add(bucketGetter.setWithInputStream(field, stream));
        return this;
    }

    @CheckReturnValue
    public <V> @NotNull BatchValueExecutor<T, P> get(Field<T, V> field, Consumer<V> executor) {
          bucket.add(bucketGetter.get(field, executor));
          return this;
    }

    @CheckReturnValue
    public <V> @NotNull BatchValueExecutor<T, P> getAsInputStream(Field<T, V> field, Consumer<InputStream> executor) {
        bucket.add(bucketGetter.getAsInputStream(field, executor));
        return this;
    }

    @CheckReturnValue
    public @NotNull BatchValueExecutor<T, P> drop() {
        bucket.add(bucketGetter.drop());
        return this;
    }

    @CheckReturnValue
    public @NotNull ExecutableFutureVoid fetch() {
        return new ExecutableFutureVoid(exposed, () -> bucket.execute().complete());
    }
}

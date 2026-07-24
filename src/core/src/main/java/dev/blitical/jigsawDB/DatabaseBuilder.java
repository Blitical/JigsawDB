package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.value.ExecutableFuture;
import org.jetbrains.annotations.CheckReturnValue;

import java.util.HashMap;
import java.util.Map;

public class DatabaseBuilder {

    protected final Driver driver;
    protected final Map<Class<? extends Table>, Table<?, ?>> tables = new HashMap<>();
    protected final Map<Class<? extends Table>, Table<?, ?>> shadowTables = new HashMap<>();

    protected CachePolicy.StaticCachePolicy cachePolicy = null;
    protected boolean ignoreInvalidShutdownWarning = false;

    public DatabaseBuilder(Driver driver) {
        this.driver = driver;
    }

    public <T extends Table<T, E>, E> DatabaseBuilder addTable(Table<T, E> table) {
        tables.put(table.getClass(), table);
        return this;
    }

    public <T extends Table<T, E>, E> DatabaseBuilder addShadowTable(Table<T, E> table) {
        shadowTables.put(table.getClass(), table);
        return this;
    }

    public DatabaseBuilder cachePolicy(
            CachePolicy.StaticCachePolicy cachePolicy
    ) {
        this.cachePolicy = cachePolicy;
        return this;
    }

    public DatabaseBuilder ignoreShutdownWarning() {
        this.ignoreInvalidShutdownWarning = true;
        return this;
    }

    public DatabaseBuilder ignoreShutdownWarning(boolean ignore) {
        this.ignoreInvalidShutdownWarning = ignore;
        return this;
    }

    @CheckReturnValue
    public ExecutableFuture<ConnectedDatabase> connect() {
        ConnectedDatabase db = new ConnectedDatabase(this);
        return new ExecutableFuture<>(db.exposed, db::connect);
    }
}

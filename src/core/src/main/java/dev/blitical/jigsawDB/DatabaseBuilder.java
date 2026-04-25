package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.value.ExecutableFuture;

import java.util.HashMap;
import java.util.Map;

public class DatabaseBuilder {

    protected final Driver driver;
    protected final Map<Class<? extends Table>, Table<?, ?>> tables = new HashMap<>();

    protected CachePolicy.StaticCachePolicy cachePolicy = null;

    public DatabaseBuilder(Driver driver) {
        this.driver = driver;
    }

    public <T extends Table<T, E>, E> DatabaseBuilder addTable(Table<T, E> table) {
        tables.put(table.getClass(), table);
        return this;
    }

    public <T extends Table<T, E>, E> DatabaseBuilder cachePolicy(
            CachePolicy.StaticCachePolicy cachePolicy
    ) {
        this.cachePolicy = cachePolicy;
        return this;
    }

    public ExecutableFuture<ConnectedDatabase> connect() {
        ConnectedDatabase db = new ConnectedDatabase(this);
        return new ExecutableFuture<>(db.exposed, db::connect);
    }
}

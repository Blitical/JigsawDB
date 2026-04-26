package dev.blitical.jigsawDB.cache;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class CacheHandler {
    private static final CachedMap CACHE = new CachedMap();

    public record CachedObject<T extends Table<T, P>, P, V>(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey,
            Field<T, V> field,
            @Nullable V value,
            boolean isCached // Some values may be null, we return this
    ) {
    }

    public static <T extends Table<T, P>, P, V> @NotNull CachedObject<T, P, @Nullable V> getCachedValue(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey,
            Field<T, V> field
    ) {
        CachedMap.CachedValue<V> value = CACHE.get(database.uuid(), table, primaryKey, field);
        CachedObject<T, P, V> val = shouldCache(database, table, field)
                ? new CachedObject<>(
                database,
                table,
                primaryKey,
                field,
                value == null ? null : value.getAndUseValue(),
                CACHE.contains(database.uuid(), table, primaryKey, field)
        ) : new CachedObject<>(database, table, primaryKey, field, null, false);

        if (value != null && value.isExpired())
            CACHE.breakValue(database.uuid(), table, primaryKey, field);
        return val;
    }

    public static <T extends Table<T, P>, P, V> void putCachedValue(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey,
            Field<T, V> field,
            V value
    ) {
        if (shouldCache(database, table, field)) {
            JigsawDBLogger.debug("Mapping cached object '%s' to value: %s", field.name(), value);
            CACHE.put(
                    database.uuid(),
                    table,
                    primaryKey,
                    field,
                    new CachedMap.CachedValue<>(
                            value,
                            getMaxCalls(database, table, field),
                            getCacheDuration(database, table, field)
                    )
            );
        }
    }

    public static <T extends Table<T, ?>> boolean shouldCache(
            ConnectedDatabase.Exposed database,
            Table<T, ?> table,
            Field<T, ?> field
    ) {
        CachePolicy<?> policy = table.getConfig().cachePolicy();

        if (policy instanceof CachePolicy.StaticCachePolicy staticCachePolicy) {
            return staticCachePolicy.selector.equals(CachePolicy.Selector.ALL);

        } else if (policy instanceof CachePolicy.CustomCachePolicy<?> customCachePolicy) {
            if (customCachePolicy.fields.contains(field)) {
                return true;
            }

        } else if (policy instanceof CachePolicy.MappedCachePolicy<?> mappedCachePolicy) {
            CachePolicy.PolicyMap<?> val = mappedCachePolicy.map.get(field);
            return val.selector.equals(CachePolicy.Selector.ALL);
        }

        CachePolicy.StaticCachePolicy databasePolicy = database.cachePolicy();
        if (databasePolicy != null) {
            return policy.selector.equals(CachePolicy.Selector.ALL);
        }
        return true; // Cache by default
    }

    public static <T extends Table<T, ?>> int getMaxCalls(
            ConnectedDatabase.Exposed database,
            Table<T, ?> table,
            Field<T, ?> field
    ) {
        CachePolicy<?> policy = table.getConfig().cachePolicy();

        if (policy instanceof CachePolicy.MappedCachePolicy<?> mappedCachePolicy) {
            CachePolicy.PolicyMap<?> val = mappedCachePolicy.map.get(field);
            if (val.maxCalls != -2) return val.maxCalls;
        }
        if (policy.maxCalls != -2)
            return policy.maxCalls;
        if (database.cachePolicy() != null && database.cachePolicy().maxCalls != -2)
            return database.cachePolicy().maxCalls;

        return -1; // Infinite by default
    }

    public static <T extends Table<T, ?>> Duration getCacheDuration(
            ConnectedDatabase.Exposed database,
            Table<T, ?> table,
            Field<T, ?> field
    ) {
        CachePolicy<?> policy = table.getConfig().cachePolicy();

        if (policy instanceof CachePolicy.MappedCachePolicy<?> mappedCachePolicy) {
            CachePolicy.PolicyMap<?> val = mappedCachePolicy.map.get(field);
            if (val.duration != null) return val.duration;
        }
        if (policy.duration != null)
            return policy.duration;
        if (database.cachePolicy() != null && database.cachePolicy().duration != null)
            return database.cachePolicy().duration;

        return null;
    }

    public static <T extends Table<T, ?>> CachePolicy.Policy getPolicy(
            ConnectedDatabase.Exposed database,
            Table<T, ?> table,
            Field<T, ?> field
    ) {
        CachePolicy<?> policy = table.getConfig().cachePolicy();

        if (policy instanceof CachePolicy.MappedCachePolicy<?> mappedCachePolicy) {
            CachePolicy.PolicyMap<?> val = mappedCachePolicy.map.get(field);
            if (val.policy != null) return val.policy;
        }
        if (policy.policy != null)
            return policy.policy;
        // EAGER by default
        return Objects.requireNonNullElse(
                database.cachePolicy() == null
                        ? null : database.cachePolicy().policy,
                CachePolicy.Policy.EAGER
        );
    }

    @SuppressWarnings("unchecked")
    public static <T extends Table<T, ?>> Field<T, ?>[] getFieldsByPolicy(
            ConnectedDatabase.Exposed database,
            Table<T, ?> table,
            CachePolicy.Policy policy
    ) {
        Set<Field<T, ?>> fields = table.getAllFields();
        Set<Field<T, ?>> result = new HashSet<>();

        for (var field : fields) {
            if (getPolicy(database, table, field).equals(policy)) {
                result.add(field);
            }
        }

        return result.toArray(new Field[0]);
    }

    public static CachedMap getCachedMap(
            ConnectedDatabase.Exposed __ // Require this so random no-names can't call this
    ) {
        return CACHE;
    }
}

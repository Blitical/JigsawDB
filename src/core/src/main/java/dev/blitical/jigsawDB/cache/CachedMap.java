package dev.blitical.jigsawDB.cache;

import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.table.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CachedMap {

    // Wow- a staircase
    // (which I should fall down on for writing this disgrace)
    private final
    Map<String, // Database id
            Map<String, // Table id
                    Map<Object, // Entry Primary Key
                            Map<String, // Column id
                                    CachedValue<?> // Value
                                    >>>> cache = new HashMap<>();
    // ^^^ If you want to complain about this, please do
    // I'm sorry for your poor eyes which have to read this abomination

    // Map of: Database id, <Table id, areAllEntriesStored?>
    private final Map<String, Map<String, Boolean>> allEntryTracker = new HashMap<>();

    public static class CachedValue<V> {
        public final V value;
        private int maxCalls;
        private LocalDateTime expires;
        private boolean expired = false;

        public CachedValue(V value, int maxCalls, Duration cacheDuration) {
            this.value = value;
            this.maxCalls = maxCalls;
            this.expires = cacheDuration == null ? null : LocalDateTime.now().plus(cacheDuration);
        }

        public V getAndUseValue() {
            if (maxCalls != -1) {
                maxCalls -= 1;
                expired = (expired || maxCalls == 0);
            }

            if (expires != null && LocalDateTime.now().isAfter(expires)) {
                expired = true;
            }

            return value;
        }

        public boolean isExpired() {
            return expired;
        }
    }

    protected CachedMap() {
    }

    @SuppressWarnings("unchecked")
    public <T extends Table<T, P>, P, V> void put(
            String databaseId,
            Table<T, P> table,
            P primaryKey,
            Field<T, V> field,
            CachedValue<V> value
    ) {
        cache.computeIfAbsent(databaseId, d -> new HashMap<>())
                .computeIfAbsent(table.getTableName(), t -> new HashMap<>())
                .computeIfAbsent(primaryKey, e -> new HashMap<>())
                .put(field.name(), value);
    }

    public <T extends Table<T, P>, P, V> boolean contains(
            String databaseId,
            Table<T, P> table,
            P primaryKey,
            Field<T, V> field
    ) {
        var tableMap = cache.get(databaseId);
        if (tableMap == null)
            return false;

        var entryMap = tableMap.get(table.getTableName());
        if (entryMap == null)
            return false;

        var columnMap = entryMap.get(primaryKey);
        if (columnMap == null)
            return false;

        return columnMap.containsKey(field.name());
    }

    @SuppressWarnings("unchecked") // We're 100% sure that it will be typesafe
    public <T extends Table<T, P>, P, V> CachedValue<V> get(
            String databaseId,
            Table<T, P> table,
            P primaryKey,
            Field<T, V> field
    ) {
        var tableMap = cache.get(databaseId);
        if (tableMap == null)
            return null;

        var entryMap = tableMap.get(table.getTableName());
        if (entryMap == null)
            return null;

        var columnMap = entryMap.get(primaryKey);
        if (columnMap == null)
            return null;

        return (CachedValue<V>) columnMap.get(field.name());
    }

    public void breakDatabase(String databaseId) {
        cache.remove(databaseId);
        allEntryTracker.remove(databaseId);
    }

    public <T extends Table<T, ?>> void breakTable(
            String databaseId,
            Table<T, ?> table
    ) {
        var tableMap = cache.get(databaseId);
        if (tableMap == null)
            return;
        tableMap.remove(table.getTableName());

        var tableMapTracker = allEntryTracker.get(databaseId);
        if (tableMapTracker == null)
            return;
        tableMapTracker.remove(table.getTableName());
    }

    public <T extends Table<T, P>, P> void breakEntry(
            String databaseId,
            Table<T, P> table,
            P primaryKey
    ) {
        var tableMap = cache.get(databaseId);
        if (tableMap == null)
            return;

        var entryMap = tableMap.get(table.getTableName());
        if (entryMap == null)
            return;

        entryMap.remove(primaryKey);
    }

    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void breakValue(
            String databaseId,
            Table<T, P> table,
            P primaryKey,
            Field<T, V> field
    ) {
        var tableMap = cache.get(databaseId);
        if (tableMap == null)
            return;

        var entryMap = tableMap.get(table.getTableName());
        if (entryMap == null)
            return;

        var columnMap = entryMap.get(primaryKey);
        if (columnMap == null)
            return;

        columnMap.remove(field.name());
    }

    public <T extends Table<T, ?>> void setAllEntryStored(
            String databaseId,
            Table<T, ?> table,
            boolean stored
    ) {
        allEntryTracker.computeIfAbsent(databaseId, d -> new HashMap<>())
                .put(table.getTableName(), stored);
    }

    public <T extends Table<T, ?>> boolean areAllEntriesStored(
            String databaseId,
            T table
    ) {
        var tableMap = allEntryTracker.get(databaseId);
        return tableMap != null && tableMap.get(table.getTableName());
    }
}

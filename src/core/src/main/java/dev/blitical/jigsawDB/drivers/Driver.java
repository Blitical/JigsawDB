package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.FieldEntry;
import dev.blitical.jigsawDB.entry.selector.EntrySelector;
import dev.blitical.jigsawDB.entry.selector.condition.Condition;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.ApiStatus;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

// @formatter:off
public abstract class Driver {
    protected Driver() {
    }

    // Basic Connection
    @ApiStatus.Internal
    public abstract void connect() throws SQLException;
    @ApiStatus.Internal
    public abstract void close() throws SQLException;
    @ApiStatus.Internal
    public abstract String formatedName();
    @ApiStatus.Internal
    public abstract boolean isOpen() throws SQLException;
    @ApiStatus.Internal
    public abstract boolean driverIsNull();

    // Manual SQL Executing
    @ApiStatus.Internal
    public abstract int execute(String sql, Object... args) throws SQLException;
    @ApiStatus.Internal
    public abstract QueryResult executeGet(String sql, Object... args) throws SQLException;

    // Table Payloads
    @ApiStatus.Internal
    public abstract Map<String, ExistingColumn> getExistingColumns(String table) throws SQLException;
    @ApiStatus.Internal
    public abstract void addColumn(String table, PredefinedColumn column) throws SQLException;

    @ApiStatus.Internal
    public abstract void renameTable(String oldTable, String newTable) throws SQLException;
    @ApiStatus.Internal
    public abstract void dropTable(String table) throws SQLException;

    @ApiStatus.Internal
    public abstract void copyData(String oldTable, String newTable, List<String> columnsToCopy) throws SQLException;

    // Column Value Payloads
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> void createEntry(Table<T, P> table, P primaryField, List<FieldEntry<T, ?, ?>> values) throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> void dropEntry(Table<T, P> table, P primaryField) throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, ?>> boolean checkEntryAndCache(Table<T, P> table, Entry<T, P> entry, F[] fields, BiConsumer<Field<T, ?>, Object> cacheHandler)throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> void set(Table<T, P> table, P primaryField, List<FieldEntry<T, ?, ?>> values) throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> V get(Table<T, P> table, P primaryField, Field<T, V> field) throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> void setWithInputStream(Table<T, P> table, P primaryField, Field<T, V> field, InputStream value) throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> InputStream getAsInputStream(Table<T, P> table, P primaryField, Field<T, V> field) throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, ?>> List<Entry<T, P>> getSpecified(ConnectedDatabase.Exposed database, Table<T, P> table, Condition<T> condition, List<EntrySelector.SortBy> sortBy, Integer limit, Set<F> fields) throws SQLException;

    // Transaction Payloads
    @ApiStatus.Internal
    public abstract void beginTransaction() throws SQLException;
    @ApiStatus.Internal
    public abstract void commitTransaction() throws SQLException;
    @ApiStatus.Internal
    public abstract void rollbackTransaction() throws SQLException;

    @ApiStatus.Internal
    public abstract void createTable(String var1, List<PredefinedColumn> var2, boolean var3) throws SQLException;
}

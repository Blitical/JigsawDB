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
@ApiStatus.Internal
public abstract class Driver {
    protected Driver() {
    }

    // Basic Connection
    public abstract void connect() throws SQLException;
    public abstract void close() throws SQLException;
    public abstract String formatedName();
    public abstract boolean isOpen() throws SQLException;
    public abstract boolean driverIsNull();

    // Manual SQL Executing
    public abstract int execute(String sql, Object... args) throws SQLException;
    public abstract QueryResult executeGet(String sql, Object... args) throws SQLException;

    // Table Payloads
    public abstract Map<String, ExistingColumn> getExistingColumns(String table) throws SQLException;
    public abstract void addColumn(String table, PredefinedColumn column) throws SQLException;

    public abstract void renameTable(String oldTable, String newTable) throws SQLException;
    public abstract void dropTable(String table) throws SQLException;

    public abstract void copyData(String oldTable, String newTable, List<String> columnsToCopy) throws SQLException;

    // Column Value Payloads
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> void createEntry(Table<T, P> table, P primaryField, List<FieldEntry<T, ?, ?>> values) throws SQLException;
    public abstract <T extends Table<T, P>, P, F extends Field<T, ?>> boolean checkEntryAndCache(Table<T, P> table, Entry<T, P> entry, F[] fields, BiConsumer<Field<T, ?>, Object> cacheHandler)throws SQLException;
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> void set(Table<T, P> table, P primaryField, List<FieldEntry<T, ?, ?>> values) throws SQLException;
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> V get(Table<T, P> table, P primaryField, Field<T, V> field) throws SQLException;
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> void setWithInputStream(Table<T, P> table, P primaryField, Field<T, V> field, InputStream value) throws SQLException;
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> InputStream getAsInputStream(Table<T, P> table, P primaryField, Field<T, V> field) throws SQLException;
    public abstract <T extends Table<T, P>, P, F extends Field<T, ?>> List<Entry<T, P>> getSpecified(ConnectedDatabase.Exposed database, Table<T, P> table, Condition<T> condition, List<EntrySelector.SortBy> sortBy, Integer limit, Set<F> fields) throws SQLException;

    // Transaction Payloads
    public abstract void beginTransaction() throws SQLException;
    public abstract void commitTransaction() throws SQLException;
    public abstract void rollbackTransaction() throws SQLException;

    public abstract void createTable(String var1, List<PredefinedColumn> var2, boolean var3) throws SQLException;
}

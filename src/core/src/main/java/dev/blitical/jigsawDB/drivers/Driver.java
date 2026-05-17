package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.drivers.action.Action;
import dev.blitical.jigsawDB.drivers.action.GetAction;
import dev.blitical.jigsawDB.drivers.action.PreparedStatementGetAction;
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
import java.sql.Connection;
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
    public abstract Connection getConnection();
    @ApiStatus.Internal
    public abstract String formatedName();
    @ApiStatus.Internal
    public abstract DriverType driverType();
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
    public abstract <T extends Table<T, ?>> Map<String, ExistingColumn> getExistingColumns(Table<T, ?> table) throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, ?>> Action addColumn(Table<T, ?> table, PredefinedColumn column);

    @ApiStatus.Internal
    public abstract Action renameTable(String oldTable, String newTable);
    @ApiStatus.Internal
    public abstract Action dropTable(String table);

    @ApiStatus.Internal
    public abstract Action copyData(String oldTable, String newTable, List<String> columnsToCopy);

    // Column Value Payloads
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> Action createEntry(Table<T, P> table, P primaryField, List<FieldEntry<T, ?, ?>> values);
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> PreparedStatementGetAction<P> createEntry(Table<T, P> table, List<FieldEntry<T, ?, ?>> values);
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> Action dropEntry(Table<T, P> table, P primaryField);
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, ?>> boolean checkEntryAndCache(Table<T, P> table, Entry<T, P> entry, F[] fields, BiConsumer<Field<T, ?>, Object> cacheHandler)throws SQLException;
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> Action set(Table<T, P> table, P primaryField, List<FieldEntry<T, ?, ?>> values);
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> GetAction<V> get(Table<T, P> table, P primaryField, Field<T, V> field);
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> Action setWithInputStream(Table<T, P> table, P primaryField, Field<T, V> field, InputStream value);
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, V>, V> GetAction<InputStream> getAsInputStream(Table<T, P> table, P primaryField, Field<T, V> field);
    @ApiStatus.Internal
    public abstract <T extends Table<T, P>, P, F extends Field<T, ?>> List<Entry<T, P>> getSpecified(ConnectedDatabase.Exposed database, Table<T, P> table, Condition<T> condition, List<EntrySelector.SortBy> sortBy, Integer limit, Set<F> fields) throws SQLException;

    // Transaction Payloads
    @ApiStatus.Internal
    public abstract Action beginTransaction();
    @ApiStatus.Internal
    public abstract Action commitTransaction();
    @ApiStatus.Internal
    public abstract Action rollbackTransaction();

    @ApiStatus.Internal
    public abstract <T extends Table<T, ?>> void createTable(ConnectedDatabase.Exposed exposed, Table<T, ?> table, List<PredefinedColumn> columns, boolean deleteUnspecifiedColumns) throws SQLException;
}

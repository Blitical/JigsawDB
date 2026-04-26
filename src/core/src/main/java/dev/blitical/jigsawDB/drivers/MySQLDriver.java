package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.FieldEntry;
import dev.blitical.jigsawDB.entry.selector.EntrySelector;
import dev.blitical.jigsawDB.entry.selector.condition.Condition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager;
import dev.blitical.jigsawDB.entry.selector.util.OrderType;
import dev.blitical.jigsawDB.table.Table;

import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MySQLDriver extends Driver {
    private final String formattedName;
    private final String url;
    private final String username;
    private final String password;
    private volatile Connection connection;

    public MySQLDriver(String host, int port, String database, String username, String password, String flags) {
        url = "jdbc:mysql://" + host + ":" + port + "/" + database + flags;
        this.username = username;
        this.password = password;
        formattedName = "MySQL@" + database;
    }

    public MySQLDriver(String host, int port, String database, String username, String password) {
        url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        this.username = username;
        this.password = password;
        formattedName = "MySQL@" + database;
    }

    public MySQLDriver(String host, String database, String username, String password) {
        url = "jdbc:mysql://" + host + ":3306/" + database;
        this.username = username;
        this.password = password;
        formattedName = "MySQL@" + database;
    }

    @Override
    public synchronized void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public String formatedName() {
        return formattedName;
    }

    @Override
    public boolean isOpen() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    @Override
    public boolean driverIsNull() {
        return connection == null;
    }

    @Override
    public int execute(String sql, Object... args) throws SQLException {
        JigsawDBLogger.sql(sql + "\nargs = " + Arrays.toString(args));
        try (PreparedStatement ps = prepare(sql, args)) {
            return ps.executeUpdate();
        }
    }

    @Override
    public QueryResult executeGet(String sql, Object... args) throws SQLException {
        JigsawDBLogger.sql(sql + "\nargs = " + Arrays.toString(args));
        PreparedStatement ps = prepare(sql, args);
        ResultSet rs = ps.executeQuery();
        return new QueryResult(ps, rs);
    }

    private PreparedStatement prepare(String sql, Object... args) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        for (int i = 0; i < args.length; ++i) {
            ps.setObject(i + 1, args[i]);
        }
        return ps;
    }

    @Override
    public Map<String, ExistingColumn> getExistingColumns(String table) throws SQLException {
        Map<String, ExistingColumn> columns = new HashMap<>();

        try (QueryResult qr = executeGet("""
                SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = ?
                """, table)) {
            ResultSet rs = qr.rs();

            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                columns.put(
                        name,
                        new ExistingColumn(
                                name,
                                rs.getString("COLUMN_TYPE"),
                                !"NO".equals(rs.getString("IS_NULLABLE")),
                                "PRI".equals(rs.getString("COLUMN_KEY"))
                        )
                );
            }
        }
        return columns;
    }

    @Override
    public void addColumn(String table, PredefinedColumn column) throws SQLException {
        execute("ALTER TABLE " + table + " ADD COLUMN " + buildColumnSql(column));
    }

    @Override
    public void renameTable(String oldTable, String newTable) throws SQLException {
        execute("RENAME TABLE " + oldTable + " TO " + newTable);
    }

    @Override
    public void dropTable(String table) throws SQLException {
        execute("DROP TABLE IF EXISTS " + table);
    }

    @Override
    public void copyData(String oldTable, String newTable, List<String> columnsToCopy) throws SQLException {
        String cols = columnsToCopy.stream()
                .map(s -> "`" + s.replace("`", "``") + "`")
                .collect(Collectors.joining(", "));
        execute(
                "INSERT INTO " + newTable
                        + " (" + cols + ") SELECT "
                        + cols + " FROM " + oldTable
        );
    }

    private String mapType(PredefinedColumn column) {
        return switch (Encoder.resolveEncodedType(column.field())) {
            case STRING -> column.primaryKey() ? "VARCHAR(768)" : "LONGTEXT";
            case BLOB -> "LONGBLOB";
            case REAL -> "DOUBLE";
            case INTEGER -> "BIGINT";
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void createEntry(
            Table<T, P> table,
            P primaryField,
            List<FieldEntry<T, ?, ?>> values
    ) throws SQLException {
        List<String> args = Stream.concat(
                Stream.of(table.getPrimaryColumnName()),
                values.stream().map((e) -> e.field().name())
        ).toList();

        List<Object> objects = new ArrayList<>();
        String placeholders = Stream.generate(() -> "?")
                .limit(args.size())
                .collect(Collectors.joining(", "));

        Encoder.EncodedObject primary = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        objects.add(primary == null ? null : primary.encoded());

        values.forEach((e) -> {
            Encoder.EncodedObject eo = Encoder.encode((V) e.value(), table, (Field<T, V>) e.field());
            objects.add(eo == null ? null : eo.encoded());
        });

        String SQL = "INSERT INTO " + table.getTableName()
                + " (" + String.join(", ", args)
                + ") VALUES (" + placeholders + ")";
        execute(SQL, objects.toArray());
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void dropEntry(
            Table<T, P> table,
            P primaryField
    ) throws SQLException {
        Encoder.EncodedObject eo = Encoder.encode(
                primaryField,
                table,
                table.getPrimaryColumn()
        );

        String SQL = "DELETE FROM `" + table.getTableName()
                + "` WHERE `" + table.getPrimaryColumnName() + "` = ?";
        execute(SQL, eo == null ? null : eo.encoded());
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, ?>> boolean checkEntryAndCache(
            Table<T, P> table,
            Entry<T, P> entry,
            F[] fields,
            BiConsumer<Field<T, ?>, Object> cacheHandler
    ) throws SQLException {
        if (fields.length == 0)
            return false;
        Set<String> names = Arrays.stream(fields)
                .map((c) -> c.name()).collect(Collectors.toSet());
        StringBuilder SQL = new StringBuilder("SELECT ")
                .append(names.stream()
                        .map(s -> "`" + s.replace("`", "``") + "`")
                        .collect(Collectors.joining(", ")))
                .append(" FROM ").append(table.getTableName())
                .append(" WHERE ").append(table.getPrimaryColumnName())
                .append(" = ?");

        Encoder.EncodedObject p = Encoder.encode(entry.primaryKey, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        try (QueryResult res = executeGet(SQL.toString(), primary)) {
            ResultSet rs = res.rs();
            if (!rs.next())
                return false;
            for (Field<T, ?> field : fields)
                cacheHandler.accept(field, Encoder.decode(rs.getObject(field.name()), table, field));
            return true;
        }
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> V get(
            Table<T, P> table,
            P primaryField,
            Field<T, V> field
    ) throws SQLException {
        String SQL = "SELECT `" + field.name()
                + "` FROM " + table.getTableName()
                + " WHERE " + table.getPrimaryColumnName() + " = ?";
        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        try (QueryResult res = executeGet(SQL, primary)) {
            ResultSet rs = res.rs();
            if (!rs.next())
                return null;
            return (V) Encoder.decode(rs.getObject(field.name()), table, field);
        }
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void
    setWithInputStream(
            Table<T, P> table,
            P primaryField,
            Field<T, V> field,
            InputStream stream
    ) throws SQLException {
        String column = "`" + field.name().replace("`", "``") + "`";

        String sql = "UPDATE " + table.getTableName() +
                " SET " + column + " = ?" +
                " WHERE `" + table.getPrimaryColumnName() + "` = ?";

        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBinaryStream(1, stream);
            ps.setObject(2, primary);
            ps.executeUpdate();
        }
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> InputStream
    getAsInputStream(
            Table<T, P> table,
            P primaryField,
            Field<T, V> field
    ) throws SQLException {
        String SQL = "SELECT `" + field.name()
                + "` FROM " + table.getTableName()
                + " WHERE " + table.getPrimaryColumnName() + " = ?";
        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        try (QueryResult res = executeGet(SQL, primary)) {
            ResultSet rs = res.rs();
            if (!rs.next())
                return null;
            return rs.getBinaryStream(field.name());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Table<T, P>, P, F extends Field<T, ?>> List<Entry<T, P>> getSpecified(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            Condition<T> condition,
            List<EntrySelector.SortBy> sortBy,
            Integer limit,
            Set<F> fields
    ) throws SQLException {
        StringBuilder SQL = new StringBuilder("SELECT ");
        List<Object> objects = new ArrayList<>();

        String names = fields.stream().map(c ->
                "`" + c.name().replace("`", "``") + "`"
        ).collect(Collectors.joining(", "));
        SQL.append(names);

        SQL.append(" FROM ").append(table.getTableName());
        if (condition != null) {
            SQL.append(" WHERE ").append(encodeCondition(condition, objects));
        }

        if (!sortBy.isEmpty()) {
            SQL.append(" ORDER BY ").append(sortBy.stream().map(s ->
                    s.field().name() + " " + (s.type().equals(OrderType.ASCENDING) ? "ASC" : "DESC")
            ).collect(Collectors.joining(", ")));
        }

        if (limit != null) {
            SQL.append(" LIMIT ").append(limit);
        }

        try (QueryResult r = executeGet(SQL.toString(), objects.toArray())) {
            ResultSet rs = r.rs();
            List<Entry<T, P>> result = new ArrayList<>();

            while (rs.next()) {
                P primaryKey = (P) rs.getObject(table.getPrimaryColumnName());
                for (Field<T, ?> field : fields)
                    putCachedValue(database, table, primaryKey, field, Encoder.decode(rs.getObject(field.name()), table, field));
                result.add(new Entry<>(database, table, primaryKey));
            }

            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Table<T, P>, P, F extends Field<T, V>, V> void putCachedValue(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey,
            Field<T, ?> field,
            Object value
    ) {
        CacheHandler.putCachedValue(database, table, primaryKey, (Field<T, V>) field, (V) value);
    }

    private <T extends Table<T, ?>> String encodeCondition(Condition<T> condition, List<Object> objects) {
        return switch (condition.getType()) {
            case COMPARISON -> {
                switch (condition) {
                    case ConditionManager.ComparisonCondition<?, ?> c -> {
                        objects.add(c.value);
                        yield c.field.name() + " " + mapComparisonType(c.type) + " ?";
                    }
                    case ConditionManager.NumberComparisonCondition<?, ?> c -> {
                        objects.add(c.value);
                        yield c.field.name() + " " + mapComparisonType(c.type) + " ?";
                    }
                    case ConditionManager.BetweenCondition<?, ?> c -> {
                        objects.add(c.min);
                        objects.add(c.max);
                        yield c.field.name() + " BETWEEN ? AND ?";
                    }
                    case ConditionManager.LikeCondition<?, ?> c -> {
                        objects.add(c.match);
                        yield c.field.name() + " LIKE ?";
                    }
                    case ConditionManager.InCondition<?, ?> c -> {
                        if (c.values.isEmpty())
                            yield "1=0";

                        String placeholders = c.values.stream()
                                .map(v -> "?")
                                .collect(Collectors.joining(", "));

                        objects.addAll(c.values);
                        yield c.field.name() + " IN (" + placeholders + ")";
                    }
                    default -> throw new IllegalStateException("Invalid ComparisonType: " + condition.getClass());
                }
            }

            case AND, OR -> {
                ConditionManager.LogicalCondition<T> logical = (ConditionManager.LogicalCondition<T>) condition;
                if (logical.children().isEmpty())
                    yield "1=1";

                String delimiter = condition.getType() == Condition.NodeType.AND ? " AND " : " OR ";
                String joined = logical.children().stream()
                        .map(child -> "(" + encodeCondition(child, objects) + ")")
                        .collect(Collectors.joining(delimiter));

                yield "(" + joined + ")";
            }

            case NOT -> {
                ConditionManager.LogicalCondition<T> logical = (ConditionManager.LogicalCondition<T>) condition;
                if (logical.children().size() != 1)
                    throw new IllegalArgumentException("NOT node must have exactly one child");

                String inner = encodeCondition(logical.children().getFirst(), objects);
                yield "(NOT " + inner + ")";
            }
        };
    }

    private String mapComparisonType(Enum<?> type) {
        return switch (type.toString()) {
            case "EQUALS" -> "=";
            case "NOT_EQUAL" -> "!=";
            case "GREATER" -> ">";
            case "GREATER_EQUAL" -> ">=";
            case "LESS" -> "<";
            case "LESS_EQUAL" -> "<=";
            default -> throw new IllegalArgumentException("Unknown comparison type: " + type);
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void set(
            Table<T, P> table,
            P primaryField,
            List<FieldEntry<T, ?, ?>> values
    ) throws SQLException {
        List<String> args = values.stream().map((e) ->
                "`" + e.field().name().replace("`", "``") + "` = ?"
        ).toList();
        List<Object> objects = new ArrayList<>();

        values.forEach((e) -> {
            Encoder.EncodedObject eo = Encoder.encode((V) e.value(), table, (Field<T, V>) e.field());
            objects.add(eo == null ? null : eo.encoded());
        });

        String SQL = "UPDATE " + table.getTableName()
                + " SET " + String.join(", ", args)
                + " WHERE `" + table.getPrimaryColumnName()
                + "` = ?";

        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();
        Object[] params = Stream.concat(objects.stream(), Stream.of(primary)).toArray();

        execute(SQL, params);
    }

    @Override
    public void beginTransaction() throws SQLException {
        execute("START TRANSACTION");
    }

    @Override
    public void commitTransaction() throws SQLException {
        execute("COMMIT");
    }

    @Override
    public void rollbackTransaction() throws SQLException {
        execute("ROLLBACK");
    }

    @Override
    public void createTable(
            String tableId,
            List<PredefinedColumn> columns,
            boolean deleteUnspecifiedColumns
    ) throws SQLException {
        beginTransaction();
        try {
            Map<String, ExistingColumn> existing = getExistingColumns(tableId);
            if (existing.isEmpty()) {
                execute(buildCreateSql(columns, tableId));
                commitTransaction();
                return;
            }

            boolean rebuildTable = false;
            Map<String, ExistingColumn> existingColumns = new HashMap<>(Map.copyOf(existing));

            for (PredefinedColumn column : columns) {
                ExistingColumn c = existingColumns.get(column.name());
                if (c == null) {
                    rebuildTable = true;
                    break;
                }

                existingColumns.remove(column.name());
                if (c.primaryKey() != column.primaryKey()
                        || c.nullable() != column.nullable()
                        || !c.type().equalsIgnoreCase(mapType(column))
                ) {
                    rebuildTable = true;
                    break;
                }
            }

            if (!existingColumns.isEmpty() && deleteUnspecifiedColumns) {
                rebuildTable = true;
            }

            if (!rebuildTable) {
                for (PredefinedColumn c : columns) {
                    if (!existing.containsKey(c.name())) {
                        addColumn(tableId, c);
                    }
                }
                commitTransaction();
                return;
            }

            String tempTable = tableId + "_tmp";
            execute(buildCreateSql(columns, tempTable));
            Objects.requireNonNull(existing);
            List<String> shared = columns.stream()
                    .map(PredefinedColumn::name)
                    .filter(existing::containsKey).toList();

            if (!shared.isEmpty())
                copyData(tableId, tempTable, shared);

            dropTable(tableId);
            renameTable(tempTable, tableId);
            commitTransaction();
        } catch (SQLException e) {
            rollbackTransaction();
            throw e;
        }
    }

    private String buildColumnSql(PredefinedColumn col) {
        StringBuilder sql = new StringBuilder("`");
        sql.append(col.name()).append("` ").append(mapType(col));
        if (!col.nullable()) {
            sql.append(" NOT NULL");
        }

        if (col.unique()) {
            sql.append(" UNIQUE");
        }

        if (col.defaultValue() != null) {
            sql.append(" DEFAULT ").append(col.formatedDefault());
        }

        if (col.primaryKey()) {
            sql.append(" PRIMARY KEY");
        }

        if (col.autoIncrement()) {
            sql.append(" AUTO_INCREMENT");
        }

        return sql.toString();
    }

    private String buildCreateSql(List<PredefinedColumn> columns, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        List<String> defs = new ArrayList<>();

        for (PredefinedColumn col : columns) {
            defs.add(buildColumnSql(col));
        }

        sql.append(String.join(", ", defs));
        sql.append(");");
        return sql.toString();
    }
}

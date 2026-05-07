package dev.blitical.jigsawDB.drivers.hierarchy;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.action.Action;
import dev.blitical.jigsawDB.drivers.action.Bucket;
import dev.blitical.jigsawDB.drivers.action.GetAction;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PermanentInputStream;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the {@code Base} class. <br>
 * In a hierarchy, all drivers will extend this class. <br>
 * This contains methods common to all drivers.
 */
public abstract class Base extends Driver {
    protected final String formattedName;
    protected final String url;
    protected volatile Connection connection;

    protected Base(String formattedName, String url) {
        this.formattedName = formattedName;
        this.url = url;
    }

    @Override
    public synchronized void close() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
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
        JigsawDBLogger.sql(sql + "\n[" + formattedName + "] args = " + Arrays.toString(args));
        try (PreparedStatement ps = prepare(sql, args)) {
            return ps.executeUpdate();
        }
    }

    @Override
    public QueryResult executeGet(String sql, Object... args) throws SQLException {
        JigsawDBLogger.sql(sql + "\n[" + formattedName + "] args = " + Arrays.toString(args));
        PreparedStatement ps = prepare(sql, args);
        ResultSet rs = ps.executeQuery();
        return new QueryResult(ps, rs);
    }

    protected PreparedStatement prepare(String sql, Object... args) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        for (int i = 0; i < args.length; ++i) {
            ps.setObject(i + 1, args[i]);
        }
        return ps;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> Action createEntry(
            Table<T, P> table,
            P primaryField,
            List<FieldEntry<T, ?, ?>> values
    ) {
        List<String> args = Stream.concat(
                Stream.of(normalize(table.getPrimaryColumnName())),
                values.stream().map((e) -> normalize(e.field().name()))
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

        String SQL = "INSERT INTO " + normalize(table.getTableName())
                + " (" + String.join(", ", args)
                + ") VALUES (" + placeholders + ")";
        return new Action(this, SQL, objects.toArray());
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> Action dropEntry(
            Table<T, P> table,
            P primaryField
    ) {
        Encoder.EncodedObject eo = Encoder.encode(
                primaryField,
                table,
                table.getPrimaryColumn()
        );

        String SQL = "DELETE FROM " + normalize(table.getTableName())
                + " WHERE " + normalize(table.getPrimaryColumnName()) + " = ?";
        return new Action(this, SQL, eo == null ? null : eo.encoded());
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
                        .map(this::normalize)
                        .collect(Collectors.joining(", ")))
                .append(" FROM ").append(normalize(table.getTableName()))
                .append(" WHERE ").append(normalize(table.getPrimaryColumnName()))
                .append(" = ?");

        Encoder.EncodedObject p = Encoder.encode(entry.primaryKey, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        try (QueryResult res = executeGet(SQL.toString(), primary)) {
            ResultSet rs = res.rs();
            if (!rs.next())
                return false;
            for (Field<T, ?> field : fields)
                cacheHandler.accept(field, Encoder.decode(getObject(rs, field.name()), table, field));
            return true;
        }
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> GetAction<V> get(
            Table<T, P> table,
            P primaryField,
            Field<T, V> field
    ) {
        String SQL = "SELECT " + normalize(field.name())
                + " FROM " + normalize(table.getTableName())
                + " WHERE " + normalize(table.getPrimaryColumnName()) + " = ?";
        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        return new GetAction<>(
                this,
                SQL,
                qr -> {
                    ResultSet rs = qr.rs();
                    if (!rs.next())
                        return null;
                    return (V) Encoder.decode(getObject(rs, field.name()), table, field);
                },
                false,
                primary
        );
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> GetAction<InputStream>
    getAsInputStream(
            Table<T, P> table,
            P primaryField,
            Field<T, V> field
    ) {
        String SQL = "SELECT " + normalize(field.name())
                + " FROM " + normalize(table.getTableName())
                + " WHERE " + normalize(table.getPrimaryColumnName()) + " = ?";
        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        return new GetAction<>(
                this,
                SQL,
                qr -> {
                    ResultSet rs = qr.rs();
                    if (!rs.next()) return null;
                    InputStream rawStream = rs.getBinaryStream(field.name());

                    return new PermanentInputStream(
                            rawStream,
                            qr.ps()
                    );
                },
                true,
                primary
        );
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

        String names = fields.stream()
                .map(c -> normalize(c.name()))
                .collect(Collectors.joining(", "));
        SQL.append(names);

        SQL.append(" FROM ").append(normalize(table.getTableName()));
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
                P primaryKey = (P) getObject(rs, table.getPrimaryColumnName());
                for (Field<T, ?> field : fields)
                    putCachedValue(database, table, primaryKey, field, Encoder.decode(getObject(rs, field.name()), table, field));
                result.add(new Entry<>(database, table, primaryKey));
            }

            return result;
        }
    }

    protected <T extends Table<T, ?>> String encodeCondition(Condition<T> condition, List<Object> objects) {
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

    protected String mapComparisonType(Enum<?> type) {
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
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> Action set(
            Table<T, P> table,
            P primaryField,
            List<FieldEntry<T, ?, ?>> values
    ) {
        List<String> args = values.stream()
                .map(e -> normalize(e.field().name()) + " = ?")
                .toList();
        List<Object> objects = new ArrayList<>();

        values.forEach((e) -> {
            Encoder.EncodedObject eo = Encoder.encode((V) e.value(), table, (Field<T, V>) e.field());
            objects.add(eo == null ? null : eo.encoded());
        });

        String SQL = "UPDATE " + normalize(table.getTableName())
                + " SET " + String.join(", ", args)
                + " WHERE " + normalize(table.getPrimaryColumnName())
                + " = ?";

        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();
        Object[] params = Stream.concat(objects.stream(), Stream.of(primary)).toArray();

        return new Action(this, SQL, params);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Table<T, P>, P, F extends Field<T, V>, V> void putCachedValue(
            ConnectedDatabase.Exposed database,
            Table<T, P> table,
            P primaryKey,
            Field<T, ?> field,
            Object value
    ) {
        CacheHandler.putCachedValue(database, table, primaryKey, (Field<T, V>) field, (V) value);
    }

    @Override
    public void createTable(
            ConnectedDatabase.Exposed exposed,
            String tableId,
            List<PredefinedColumn> columns,
            boolean deleteUnspecifiedColumns
    ) throws SQLException {
        final Bucket bucket = new Bucket(exposed);
        Map<String, ExistingColumn> existing = getExistingColumns(tableId);
        if (existing.isEmpty()) {
            bucket.add(new Action(this, buildCreateSql(columns, tableId)));
            bucket.execute().complete();
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
                    bucket.add(addColumn(tableId, c));
                }
            }
            bucket.execute().complete();
            return;
        }

        String tempTable = tableId + "_tmp";
        bucket.add(new Action(this, buildCreateSql(columns, tempTable)));
        Objects.requireNonNull(existing);
        List<String> shared = columns.stream()
                .map(PredefinedColumn::name)
                .filter(existing::containsKey).toList();
        if (!shared.isEmpty()) {
            bucket.add(copyData(tableId, tempTable, shared));
        }

        bucket.add(dropTable(tableId));
        bucket.add(renameTable(tempTable, tableId));
        bucket.execute().complete();
    }

    @Override
    public Action copyData(String oldTable, String newTable, List<String> columnsToCopy) {
        String cols = columnsToCopy.stream()
                .map(this::normalize)
                .collect(Collectors.joining(", "));
        return new Action(
                this,
                "INSERT INTO " + normalize(newTable)
                + " (" + cols + ") SELECT " + cols
                + " FROM " + normalize(oldTable)
        );
    }

    @Override
    public Action commitTransaction() {
        return new Action(this, "COMMIT");
    }

    @Override
    public Action rollbackTransaction() {
        return new Action(this, "ROLLBACK");
    }

    protected abstract String buildCreateSql(List<PredefinedColumn> columns, String tableName);
    protected abstract String mapType(PredefinedColumn column);

    public Object getObject(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName);
    }

    public String normalize(String input) {
        return "`" + input.replace("`", "``") + "`";
    }
}

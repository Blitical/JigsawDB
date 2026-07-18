package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.action.Action;
import dev.blitical.jigsawDB.drivers.hierarchy.Base;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.drivers.types.TypeDefinition;
import dev.blitical.jigsawDB.drivers.types.TypeDefinitionResolver;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.encoder.ParseType;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.FieldEntry;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.Math.clamp;

@ApiStatus.Experimental
public class PostgreSQLDriver extends Base {
    private final String username;
    private final String password;

    public PostgreSQLDriver(String host, int port, String database, String username, String password, String flags) {
        super(
                "PostgreSQL@" + database,
                "jdbc:postgresql://" + host + ":" + port + "/" + database + flags
        );
        this.username = username;
        this.password = password;
    }

    public PostgreSQLDriver(String host, int port, String database, String username, String password) {
        super(
                "PostgreSQL@" + database,
                "jdbc:postgresql://" + host + ":" + port + "/" + database
        );
        this.username = username;
        this.password = password;
    }

    public PostgreSQLDriver(String host, String database, String username, String password) {
        super(
                "PostgreSQL@" + database,
                "jdbc:postgresql://" + host + ":5432/" + database
        );
        this.username = username;
        this.password = password;
    }

    @Override
    public synchronized void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
    }

    @Override
    public DriverType driverType() {
        return DriverType.PostgreSQL;
    }

    @Override
    public <T extends Table<T, ?>> Map<String, ExistingColumn> getExistingColumns(Table<T, ?> tbl) throws SQLException {
        String table = tbl.getTableName();
        Map<String, ExistingColumn> columns = new HashMap<>();

        try (QueryResult qr = executeGet("""
                SELECT c.column_name,
                       c.data_type,
                       c.is_nullable,
                       EXISTS (
                           SELECT 1
                           FROM information_schema.table_constraints tc
                           JOIN information_schema.key_column_usage kcu
                             ON tc.constraint_name = kcu.constraint_name
                            AND tc.table_schema = kcu.table_schema
                            AND tc.table_name = kcu.table_name
                           WHERE tc.constraint_type = 'PRIMARY KEY'
                             AND tc.table_catalog = c.table_catalog
                             AND tc.table_schema = c.table_schema
                             AND tc.table_name = c.table_name
                             AND kcu.column_name = c.column_name
                       ) AS primary_key
                FROM information_schema.columns c
                WHERE c.table_catalog = current_database()
                AND c.table_name = ?
                """, table)) {
            ResultSet rs = qr.rs();

            while (rs.next()) {
                String name = rs.getString("column_name");
                columns.put(
                        name,
                        new ExistingColumn(
                                name,
                                rs.getString("data_type"),
                                "YES".equals(rs.getString("is_nullable")),
                                rs.getBoolean("primary_key")
                        )
                );
            }
        }
        return columns;
    }

    @Override
    public <T extends Table<T, ?>> Action addColumn(Table<T, ?> table, PredefinedColumn column) {
        return new Action(this, "ALTER TABLE " + normalize(table.getTableName()) + " ADD COLUMN " + buildColumnSql(table, column));
    }

    @Override
    public Action renameTable(String oldTable, String newTable) {
        return new Action(this, "ALTER TABLE " + normalize(oldTable) + " RENAME TO " + normalize(newTable));
    }

    @Override
    public Action dropTable(String table) {
        return new Action(this, "DROP TABLE IF EXISTS " + normalize(table));
    }

    @Override
    public Action beginTransaction() {
        return new Action(this, "BEGIN");
    }

    @Override
    protected <T extends Table<T, ?>> String mapType(Table<T, ?> table, PredefinedColumn column) {
        TypeDefinition type = TypeDefinitionResolver.resolve(table, column.name());

        return switch (type.type()) {
            case VARCHAR -> {
                int max = column.primaryKey() ? 768 : 16383;
                int len = type.length() != null ? type.length() : 255;
                yield "VARCHAR(" + clamp(len, 1, max) + ")";
            }

            case CHAR -> {
                int len = type.length() != null ? type.length() : 1;
                yield "CHAR(" + clamp(len, 1, 255) + ")";
            }

            case DECIMAL, NUMERIC -> {
                int p = type.precision() != null ? type.precision() : 10;
                int s = type.scale() != null ? type.scale() : 0;
                yield "NUMERIC(" + p + "," + s + ")";
            }

            case TEXT, TINYTEXT, MEDIUMTEXT, LONGTEXT, JSON -> "TEXT";
            case UUID -> "VARCHAR(36)";
            case TINYINT, SMALLINT -> "SMALLINT";
            case MEDIUMINT, INTEGER -> "INTEGER";
            case FLOAT -> "REAL";
            case DOUBLE -> "DOUBLE PRECISION";
            case BINARY, VARBINARY, BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB -> "BYTEA";
            case BOOLEAN, BIGINT, BIT -> type.type().name();
        };
    }

    @Override
    protected <T extends Table<T, ?>> boolean columnTypeMatches(
            Table<T, ?> table,
            PredefinedColumn column,
            ExistingColumn existing
    ) {
        String expected = mapType(table, column).toLowerCase(Locale.ROOT);
        String actual = existing.type().toLowerCase(Locale.ROOT);

        if (expected.startsWith("varchar")) {
            return actual.equals("character varying") || actual.equals("varchar");
        }
        if (expected.startsWith("numeric")) {
            return actual.equals("numeric") || actual.equals("decimal");
        }
        return actual.equals(expected);
    }

    @Override
    protected <T extends Table<T, ?>> String buildCreateSql(Table<T, ?> table, List<PredefinedColumn> columns, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ")
                .append(normalize(tableName))
                .append(" (");
        List<String> defs = new ArrayList<>();

        for (PredefinedColumn col : columns) {
            defs.add(buildColumnSql(table, col));
        }

        sql.append(String.join(", ", defs));
        sql.append(")");
        return sql.toString();
    }

    private <T extends Table<T, ?>> String buildColumnSql(Table<T, ?> table, PredefinedColumn col) {
        StringBuilder sql = new StringBuilder()
                .append(normalize(col.name()))
                .append(" ")
                .append(mapType(table, col));

        if (col.primaryKey()) {
            sql.append(" PRIMARY KEY");
        }

        if (!col.primaryKey() && !col.nullable()) {
            sql.append(" NOT NULL");
        }

        if (col.unique()) {
            sql.append(" UNIQUE");
        }

        if (col.defaultValue() != null) {
            sql.append(" DEFAULT ").append(col.formatedDefault());
        }

        if (col.autoIncrement()) {
            sql.append(" GENERATED BY DEFAULT AS IDENTITY");
        }

        return sql.toString();
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

        for (var e : values) {
            V value = (V) e.value();

            if (value instanceof String string) {
                java.lang.reflect.Field field = table.retrieveColumnReflectField(e.field());
                ParseType type = Encoder.resolveParseType(field);

                if (type == null || type.equals(ParseType.STRING)) {
                    int index = string.indexOf('\0');
                    if (index >= 0) {
                        JigsawDBLogger.warn("""
                                Your string contains a NUL character at index %s.
                                PostgreSQL does NOT support NUL characters. This will be stripped.
                                Please sanitize your input before storing NUL characters.
                                If you would like you store strings, parse your input as a byte[] using ParseType.BINARY
                                Your string: %s""", index, string);
                        value = (V) string.replace("\u0000", "");
                    }
                }
            }

            Encoder.EncodedObject eo = Encoder.encode(value, table, (Field<T, V>) e.field());
            objects.add(eo == null ? null : eo.encoded());
        }

        String SQL = "UPDATE " + normalize(table.getTableName())
                + " SET " + String.join(", ", args)
                + " WHERE " + normalize(table.getPrimaryColumnName())
                + " = ?";

        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();
        Object[] params = Stream.concat(objects.stream(), Stream.of(primary)).toArray();

        return new Action(this, SQL, params);
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> Action setWithInputStream(
            Table<T, P> table,
            P primaryField,
            Field<T, V> field,
            InputStream stream
    ) {

        String sql = "UPDATE " + normalize(table.getTableName()) +
                " SET " + normalize(field.name()) + " = ?" +
                " WHERE " + normalize(table.getPrimaryColumnName()) + " = ?";

        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        return new Action(this, sql, ps -> {
            try {
                ps.setBytes(1, stream.readAllBytes());
                ps.setObject(2, primary);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public String normalize(String input) {
        return "\"" + input.replace("\"", "\"\"") + "\"";
    }
}

package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.hierarchy.Base;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.encoder.ParseType;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.FieldEntry;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.blitical.jigsawDB.encoder.Encoder.resolveParseType;

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
    public Map<String, ExistingColumn> getExistingColumns(String table) throws SQLException {
        Map<String, ExistingColumn> columns = new HashMap<>();

        try (QueryResult qr = executeGet("""
                SELECT column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_catalog = current_database()
                AND table_name = ?
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
                                false
                        )
                );
            }
        }
        return columns;
    }

    @Override
    public void addColumn(String table, PredefinedColumn column) throws SQLException {
        execute("ALTER TABLE " + normalize(table) + " ADD COLUMN " + buildColumnSql(column));
    }

    @Override
    public void renameTable(String oldTable, String newTable) throws SQLException {
        execute("ALTER TABLE " + normalize(oldTable) + " RENAME TO " + normalize(newTable));
    }

    @Override
    public void dropTable(String table) throws SQLException {
        execute("DROP TABLE IF EXISTS " + normalize(table));
    }

    @Override
    public void beginTransaction() throws SQLException {
        execute("BEGIN");
    }

    @Override
    protected String mapType(PredefinedColumn column) {
        return switch (Encoder.resolveEncodedType(column.field())) {
            case STRING -> column.primaryKey() ? "VARCHAR(255)" : "TEXT";
            case BLOB -> "BYTEA";
            case REAL -> "DOUBLE PRECISION";
            case INTEGER -> "BIGINT";
        };
    }

    @Override
    protected String buildCreateSql(List<PredefinedColumn> columns, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ")
                .append(normalize(tableName))
                .append(" (");
        List<String> defs = new ArrayList<>();

        for (PredefinedColumn col : columns) {
            defs.add(buildColumnSql(col));
        }

        sql.append(String.join(", ", defs));
        sql.append(")");
        return sql.toString();
    }

    private String buildColumnSql(PredefinedColumn col) {
        StringBuilder sql = new StringBuilder()
                .append(normalize(col.name()))
                .append(" ")
                .append(mapType(col));

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
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void set(
            Table<T, P> table,
            P primaryField,
            List<FieldEntry<T, ?, ?>> values
    ) throws SQLException {
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

        execute(SQL, params);
    }

    @Override
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void setWithInputStream(
            Table<T, P> table,
            P primaryField,
            Field<T, V> field,
            InputStream stream
    ) throws SQLException {

        String sql = "UPDATE " + normalize(table.getTableName()) +
                " SET " + normalize(field.name()) + " = ?" +
                " WHERE " + normalize(table.getPrimaryColumnName()) + " = ?";

        Encoder.EncodedObject p = Encoder.encode(primaryField, table, table.getPrimaryColumn());
        Object primary = p == null ? null : p.encoded();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, stream.readAllBytes());
            ps.setObject(2, primary);
            ps.executeUpdate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String normalize(String input) {
        return "\"" + input.replace("\"", "\"\"") + "\"";
    }
}

package dev.blitical.jigsawDB.drivers.hierarchy;

import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.table.Table;

import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is the {@code Base} class. <br>
 * In a hierarchy, {@code MySQLDriver} and {@code MariaDBDriver} will extend this class. <br>
 * This contains methods common to all drivers.
 */
public abstract class MySQLikeDriver extends Base {
    private final String username;
    private final String password;

    protected MySQLikeDriver(
            String formattedName,
            String url,
            String username,
            String password
    ) {
        super(formattedName, url);
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
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> void
    setWithInputStream(
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
            ps.setBinaryStream(1, stream);
            ps.setObject(2, primary);
            ps.executeUpdate();
        }
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
        execute("ALTER TABLE " + normalize(table) + " ADD COLUMN " + buildColumnSql(column));
    }

    protected String buildColumnSql(PredefinedColumn col) {
        StringBuilder sql = new StringBuilder(normalize(col.name()))
                .append(" ").append(mapType(col));

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

    @Override
    protected String buildCreateSql(List<PredefinedColumn> columns, String tableName) {
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

    @Override
    protected String mapType(PredefinedColumn column) {
        return switch (Encoder.resolveEncodedType(column.field())) {
            case STRING -> column.primaryKey() ? "VARCHAR(768)" : "LONGTEXT";
            case BLOB -> "LONGBLOB";
            case REAL -> "DOUBLE";
            case INTEGER -> "BIGINT";
        };
    }

    @Override
    public void renameTable(String oldTable, String newTable) throws SQLException {
        execute("RENAME TABLE " + normalize(oldTable) + " TO " + normalize(newTable));
    }

    @Override
    public void dropTable(String table) throws SQLException {
        execute("DROP TABLE IF EXISTS " + normalize(table));
    }

    public void beginTransaction() throws SQLException {
        execute("START TRANSACTION");
    }
}

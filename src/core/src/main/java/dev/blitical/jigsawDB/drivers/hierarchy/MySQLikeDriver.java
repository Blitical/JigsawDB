package dev.blitical.jigsawDB.drivers.hierarchy;

import dev.blitical.jigsawDB.drivers.action.Action;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.drivers.types.TypeDefinition;
import dev.blitical.jigsawDB.drivers.types.TypeDefinitionResolver;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.table.Table;

import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.clamp;

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
    public <T extends Table<T, P>, P, F extends Field<T, V>, V> Action
    setWithInputStream(
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
            ps.setBinaryStream(1, stream);
            ps.setObject(2, primary);
        });
    }

    @Override
    public <T extends Table<T, ?>> Map<String, ExistingColumn> getExistingColumns(Table<T, ?> tbl) throws SQLException {
        String table = tbl.getTableName();
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
    public <T extends Table<T, ?>> Action addColumn(Table<T, ?> table, PredefinedColumn column) {
        return new Action(this, "ALTER TABLE " + normalize(table.getTableName()) + " ADD COLUMN " + buildColumnSql(table, column));
    }

    protected <T extends Table<T, ?>> String buildColumnSql(Table<T, ?> table, PredefinedColumn col) {
        StringBuilder sql = new StringBuilder(normalize(col.name()))
                .append(" ").append(mapType(table, col));

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
    protected <T extends Table<T, ?>> String buildCreateSql(Table<T, ?> table, List<PredefinedColumn> columns, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(normalize(tableName)).append(" (");
        List<String> defs = new ArrayList<>();

        for (PredefinedColumn col : columns) {
            defs.add(buildColumnSql(table, col));
        }

        sql.append(String.join(", ", defs));
        sql.append(");");
        return sql.toString();
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

            case BINARY -> {
                int len = type.length() != null ? type.length() : 1;
                yield "BINARY(" + clamp(len, 1, Integer.MAX_VALUE) + ")";
            }

            case VARBINARY -> {
                int len = type.length() != null ? type.length() : 255;
                yield "VARBINARY(" + clamp(len, 1, Integer.MAX_VALUE) + ")";
            }

            case DECIMAL -> {
                int p = type.precision() != null ? type.precision() : 10;
                int s = type.scale() != null ? type.scale() : 0;
                yield "DECIMAL(" + p + "," + s + ")";
            }

            case NUMERIC -> {
                int p = type.precision() != null ? type.precision() : 10;
                int s = type.scale() != null ? type.scale() : 0;
                yield "NUMERIC(" + p + "," + s + ")";
            }

            case UUID -> "CHAR(36)";

            case TEXT, TINYTEXT, MEDIUMTEXT, LONGTEXT, BLOB,
                 TINYBLOB, MEDIUMBLOB, LONGBLOB, TINYINT, INTEGER,
                 SMALLINT, MEDIUMINT, BIGINT, FLOAT, DOUBLE, BIT,
                 BOOLEAN, JSON -> type.type().name();
        };
    }

    @Override
    public Action renameTable(String oldTable, String newTable) {
        return new Action(this, "RENAME TABLE " + normalize(oldTable) + " TO " + normalize(newTable));
    }

    @Override
    public Action dropTable(String table) {
        return new Action(this, "DROP TABLE IF EXISTS " + normalize(table));
    }

    public Action beginTransaction() {
        return new Action(this, "START TRANSACTION");
    }
}

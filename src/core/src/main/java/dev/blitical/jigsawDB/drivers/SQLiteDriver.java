package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.drivers.action.Action;
import dev.blitical.jigsawDB.drivers.hierarchy.Base;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.table.Table;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLiteDriver extends Base {
    public SQLiteDriver(String path) {
        this(Paths.get(path));
    }

    public SQLiteDriver(Path path) {
        Path p = resolveAndValidate(path);
        super(
                "SQLite@" + formatName(p),
                "jdbc:sqlite:" + p.toAbsolutePath()
        );
    }

    public SQLiteDriver(String databaseName, String... dirs) {
        this(Paths.get("", dirs).resolve(databaseName));
    }

    @Override
    public synchronized void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url);
        }
    }

    @Override
    public DriverType driverType() {
        return DriverType.SQLite;
    }

    private static Path resolveAndValidate(Path path) {
        try {
            Path absolute = path.toAbsolutePath().normalize();
            Path parent = absolute.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(absolute)) {
                Files.createFile(absolute);
            }

            return absolute;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to access or create SQLite database at: " + path, e);
        }
    }

    private static String formatName(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        int count = absolute.getNameCount();
        if (count <= 2) {
            return ".../" + absolute.toString().replace("\\", "/");
        }

        Path lastTwo = absolute.subpath(count - 2, count);
        return ".../" + lastTwo.toString().replace("\\", "/");
    }


    @Override
    public <T extends Table<T, ?>> Map<String, ExistingColumn> getExistingColumns(Table<T, ?> tbl) throws SQLException {
        String table = tbl.getTableName();
        Map<String, ExistingColumn> columns = new HashMap<>();
        try (QueryResult qr = executeGet("PRAGMA table_info(" + normalize(table) + ")")) {
            ResultSet rs = qr.rs();
            while (rs.next()) {
                String name = rs.getString("name");
                columns.put(
                        name,
                        new ExistingColumn(
                                name,
                                rs.getString("type"),
                                rs.getInt("notnull") != 1,
                                rs.getInt("pk") == 1
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
        return new Action(this, "DROP TABLE " + normalize(table));
    }

    @Override
    protected <T extends Table<T, ?>> String mapType(Table<T, ?> table, PredefinedColumn column) {
        return switch (Encoder.resolveEncodedType(column.field())) {
            case STRING -> "STRING";
            case BLOB -> "BLOB";
            case REAL -> "REAL";
            case INTEGER -> "INTEGER";
        };
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
            try {
                ps.setBytes(1, stream.readAllBytes());
                ps.setObject(2, primary);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Action beginTransaction() {
        return new Action(this, "BEGIN TRANSACTION");
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
        sql.append(");");
        return sql.toString();
    }

    private <T extends Table<T, ?>> String buildColumnSql(Table<T, ?> table, PredefinedColumn col) {
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
            sql.append(" AUTOINCREMENT");
        }

        return sql.toString();
    }
}

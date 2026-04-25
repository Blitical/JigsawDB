package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.FieldEntry;
import dev.blitical.jigsawDB.entry.selector.WithWhere;
import dev.blitical.jigsawDB.exceptions.compile.DuplicatePrimaryColumnException;
import dev.blitical.jigsawDB.exceptions.compile.NoPrimaryColumnException;
import dev.blitical.jigsawDB.table.ColumnConfig;
import dev.blitical.jigsawDB.table.DefinedColumnConfig;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.table.TableConfig;
import dev.blitical.jigsawDB.value.ExecutableFuture;
import dev.blitical.jigsawDB.value.ExecutableFutureNullable;
import dev.blitical.jigsawDB.value.QueueManager;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectedDatabase {
    private static final Set<ConnectedDatabase> CONNECTED_DATABASES = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!CONNECTED_DATABASES.isEmpty()) {
                StringBuilder formatted = new StringBuilder();
                CONNECTED_DATABASES.forEach((d) -> formatted.append("\n    - ").append(d.driver.formatedName()));
                JigsawDBLogger.warn("""
                                You haven't shut down your databases correctly:
                                ===============================================
                                YOU HAVEN'T SHUT DOWN YOUR DATABASES CORRECTLY!
                                The following databases were not shutdown: %s
                                This could lead to:
                                    - Corrupted Data
                                    - Incomplete/Unexecuted queued actions
                                    - Database Locks
                                -- PLEASE SHUT DOWN YOUR DATABASE CORRECTLY --
                                Use ConnectedDatabase#awaitShutdown();
                                See more info at ...
                                We will attempt to shutdown these databases now.
                                ===============================================""",
                        formatted
                );

                try {
                    CONNECTED_DATABASES.forEach(ConnectedDatabase::awaitShutdown);
                    JigsawDBLogger.info("Successfully force-shutdown all databases");
                } catch (Throwable t) {
                    JigsawDBLogger.severe(t, "Failed to shutdown databases");
                }
            }

        }));
    }

    private final String uuid = UUID.randomUUID().toString();
    private final Driver driver;
    private final Map<Class<? extends Table>, Table<?, ?>> tables;
    private final CachePolicy.StaticCachePolicy cachePolicy;
    // This allows other internal classes to use the exposed data
    protected final Exposed exposed;
    private final QueueManagerStore queueManager;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public record Exposed(
            String uuid,
            ConnectedDatabase database,
            Driver driver,
            Map<Class<? extends Table>, Table<?, ?>> tables,
            CachePolicy.StaticCachePolicy cachePolicy,
            QueueManagerStore queueManager
    ) {
        public boolean isOpen() {
            return database.isOpen();
        }
    }

    public static class QueueManagerStore {
        private QueueManager queueManager = null;

        private void set(QueueManager manager) {
            this.queueManager = manager;
        }

        public QueueManager get() {
            return queueManager;
        }
    }

    protected ConnectedDatabase(DatabaseBuilder builder) {
        this.driver = builder.driver;
        this.tables = builder.tables;
        this.cachePolicy = builder.cachePolicy;
        this.queueManager = new QueueManagerStore();
        this.exposed = new Exposed(
                uuid, this, driver, tables, cachePolicy, queueManager
        );
        this.queueManager.set(new QueueManager(exposed));
    }

    protected ConnectedDatabase connect() {
        try {
            driver.connect();
            for (Map.Entry<Class<? extends Table>, Table<?, ?>> entry : tables.entrySet()) {
                createTable(entry.getKey(), entry.getValue());
            }
            CONNECTED_DATABASES.add(this);
            JigsawDBLogger.info("Database '%s' has initialised successfully", driver.formatedName());
            return this;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTable(Class<? extends Table> clazz, Table<?, ?> table) throws SQLException {
        List<PredefinedColumn> predefinedColumns = new ArrayList<>();
        TableConfig.Exposed<? extends Table<?, ?>> config = table.getConfig();

        int primaryKeyCount = 0;
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class))
                continue;

            Column annotation = field.getAnnotation(Column.class);
            DefinedColumnConfig<?> columnConfig = new ColumnConfig<>().asDefinedConfig();

            for (Map.Entry<
                    ? extends dev.blitical.jigsawDB.entry.Field<? extends Table<?, ?>, ?>,
                    ColumnConfig<?>
                    > c : config.columns().entrySet()) {
                if (annotation.value().equals(c.getKey().name())) {
                    columnConfig = c.getValue().asDefinedConfig();
                }
            }

            boolean primaryKey = false;
            if (field.isAnnotationPresent(PrimaryColumn.class)) {
                primaryKey = true;
                primaryKeyCount += 1;
            }

            predefinedColumns.add(new PredefinedColumn(
                    field.getAnnotation(Column.class).value(),
                    field,
                    columnConfig.defaultValue(),
                    formatDefault(field, columnConfig),
                    columnConfig.nullable(),
                    columnConfig.unique(),
                    columnConfig.autoIncrement(),
                    primaryKey
            ));
        }

        if (primaryKeyCount == 0) {
            throw new NoPrimaryColumnException(clazz.getSimpleName());
        }
        if (primaryKeyCount > 1) {
            throw new DuplicatePrimaryColumnException(clazz.getSimpleName());
        }

        driver.createTable(
                table.getTableName(),
                predefinedColumns,
                true
        );
    }

    private <T> String formatDefault(Field field, DefinedColumnConfig<T> columnConfig) {
        T defaultValue = columnConfig.defaultValue();
        if (defaultValue == null) return "NULL";

        return switch (Encoder.resolveParseType(field)) {
            case INTEGER, REAL, ENUM_ORDINAL, TEMPORAL_EPOCH, TEMPORAL_ISO -> defaultValue.toString();
            case STRING, UUID_STRING, ENUM_STRING -> "'" + defaultValue.toString().replace("'", "''") + "'";
            case JSON, JAVA_SERIALIZED, BLOB -> "NULL"; // Default value handled by us
        };
    }

    protected boolean isOpen() {
        try {
            return !this.shuttingDown.get() && this.driver.isOpen();
        } catch (SQLException e) {
            JigsawDBLogger.severe(e, "Failed check database '%s' open state", this.driver.formatedName());
            return false;
        }
    }

    public void awaitShutdown() {
        this.shuttingDown.set(true);
        this.queueManager.get().awaitShutdown();

        try {
            this.driver.close();
            CONNECTED_DATABASES.remove(this);
            JigsawDBLogger.info("Disconnected from database '%s'", this.driver.formatedName());
        } catch (SQLException e) {
            JigsawDBLogger.severe(e, "Failed to shutdown database '%s'", this.driver.formatedName());
            throw new RuntimeException(e);
        }
    }

    public void asyncShutdown() {
        this.shuttingDown.set(true);
        CompletableFuture.runAsync(this::awaitShutdown);
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> ExecutableFutureNullable<@Nullable Entry<T, P>>
    getEntry(Class<T> clazz, P id) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            return new ExecutableFutureNullable<>(exposed, () -> null);

        return new ExecutableFutureNullable<>(exposed, () -> {
            var entry = new Entry<>(exposed, table, id, false);
            return entry.exists ? entry : null;
        });
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> ExecutableFuture<@NotNull Entry<T, P>>
    getOrCreateEntry(Class<T> clazz, P id) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            return new ExecutableFuture<>(exposed, () -> null);

        return new ExecutableFuture<>(exposed, () ->
                new Entry<>(exposed, table, id, true)
        );
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> WithWhere<T, P>
    selectEntries(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            return null;

        return new WithWhere<>(exposed, table);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public <T extends Table<T, P>, P> ExecutableFuture<@NotNull Entry<T, P>>
    createEntry(Class<T> clazz, P id) {
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            return null;

        return new ExecutableFutureNullable<>(
                exposed,
                () -> {
                    try {
                        List<FieldEntry<T, ?, ?>> values = new ArrayList<>();

                        table.getConfig().columns().forEach((f, c) -> {
                            var defaultValue = c.asDefinedConfig().defaultValue();
                            if (defaultValue != null) {
                                values.add(
                                        new FieldEntry<>(
                                                table,
                                                (dev.blitical.jigsawDB.entry.Field<T, Object>) f,
                                                defaultValue
                                        )
                                );
                            }
                        });

                        driver.createEntry(table, id, values);

                        table.getConfig().columns().forEach((f, c) -> {
                            var defaultValue = c.asDefinedConfig().defaultValue();
                            if (defaultValue != null) {
                                CacheHandler.putCachedValue(
                                        exposed,
                                        table,
                                        id,
                                        (dev.blitical.jigsawDB.entry.Field<T, Object>) f,
                                        defaultValue
                                );
                            }
                        });

                        return new Entry<>(exposed, table, id, false);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}

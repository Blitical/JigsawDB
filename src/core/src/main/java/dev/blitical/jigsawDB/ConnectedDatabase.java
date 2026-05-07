package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.cache.CachedMap;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.DriverType;
import dev.blitical.jigsawDB.drivers.action.Bucket;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.FieldEntry;
import dev.blitical.jigsawDB.entry.InitialValueExecutor;
import dev.blitical.jigsawDB.entry.selector.WithWhere;
import dev.blitical.jigsawDB.exceptions.compile.DuplicatePrimaryColumnException;
import dev.blitical.jigsawDB.exceptions.compile.NoPrimaryColumnException;
import dev.blitical.jigsawDB.exceptions.initialization.DuplicateTableException;
import dev.blitical.jigsawDB.exceptions.runtime.TableNotInitializedException;
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
import java.util.function.Function;

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
    private final Map<Class<? extends Table>, Table<?, ?>> regularTables;
    private final Map<Class<? extends Table>, Table<?, ?>> shadowTables;
    private final CachePolicy.StaticCachePolicy cachePolicy;
    private final CachedMap cachedMap;
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

        Map<Class<? extends Table>, Table<?, ?>> allTables = new HashMap<>(builder.shadowTables);
        for (var entry : builder.tables.entrySet()) {
            allTables.put(entry.getKey(), entry.getValue());
            if (builder.shadowTables.containsKey(entry.getKey())) {
                throw new DuplicateTableException();
            }
        }

        this.tables = Map.copyOf(allTables);
        this.regularTables = Map.copyOf(builder.tables);
        this.shadowTables = Map.copyOf(builder.shadowTables);

        this.cachePolicy = builder.cachePolicy;
        this.queueManager = new QueueManagerStore();
        this.exposed = new Exposed(
                uuid, this, driver, tables, cachePolicy, queueManager
        );
        this.queueManager.set(new QueueManager(exposed));
        this.cachedMap = CacheHandler.getCachedMap(exposed);
    }

    protected ConnectedDatabase connect() {
        try {
            driver.connect();
            for (var entry : regularTables.entrySet()) {
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
                exposed,
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
            case JSON, JAVA_SERIALIZED, BLOB, BINARY -> "NULL"; // Default value handled by us
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

    public String getFormattedName() {
        return driver.formatedName();
    }

    public DriverType getDriverType() {
        return driver.driverType();
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

    public Bucket createBucket() {
        return new Bucket(exposed);
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> ExecutableFutureNullable<@Nullable Entry<T, P>>
    getEntry(Class<T> clazz, P id) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            return new ExecutableFutureNullable<>(exposed, () -> null);

        return new ExecutableFutureNullable<>(exposed, () -> {
            var entry = new Entry<>(exposed, table, id, false, null);
            return entry.exists ? entry : null;
        });
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> @NotNull ExecutableFuture<@NotNull Entry<T, P>>
    getOrCreateEntry(Class<T> clazz, P id) {
        return getOrCreateEntry(clazz, id, null);
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> @NotNull ExecutableFuture<@NotNull Entry<T, P>>
    getOrCreateEntry(Class<T> clazz, P id, Function<InitialValueExecutor<T, P>, InitialValueExecutor.Built<T, P>> initialValues) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            throw new TableNotInitializedException(clazz.getSimpleName());

        return new ExecutableFuture<>(exposed, () ->
            new Entry<>(exposed, table, id, true, initialValues)
        );
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> @NotNull WithWhere<T, P>
    selectEntries(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            throw new TableNotInitializedException(clazz.getSimpleName());

        return new WithWhere<>(exposed, table);
    }

    @CheckReturnValue
    public <T extends Table<T, P>, P> @NotNull ExecutableFuture<@NotNull Entry<T, P>>
    createEntry(Class<T> clazz, P id) {
        return createEntry(clazz, id, null);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public <T extends Table<T, P>, P> @NotNull ExecutableFuture<@NotNull Entry<T, P>>
    createEntry(Class<T> clazz, P id, Function<InitialValueExecutor<T, P>, InitialValueExecutor.Built<T, P>> initialValuesBuilder) {
        Table<T, P> table = (Table<T, P>) tables.get(clazz);

        if (table == null)
            throw new TableNotInitializedException(clazz.getSimpleName());

        return new ExecutableFutureNullable<>(
                exposed,
                () -> {
                    List<FieldEntry<T, ?, ?>> values = new ArrayList<>();

                    Map<String, InitialValueExecutor.InitialValue<T, ?>> initialValues
                            = initialValuesBuilder == null
                            ? Map.of()
                            : initialValuesBuilder.apply(new InitialValueExecutor<>(exposed)).values();

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

                    initialValues.forEach((_, iv) -> {
                        if (iv != null) {
                            values.add(
                                    new FieldEntry<>(
                                            table,
                                            (dev.blitical.jigsawDB.entry.Field<T, Object>) iv.field(),
                                            iv.value()
                                    )
                            );
                        }
                    });

                    driver.createEntry(table, id, values).execute(exposed);

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

                    initialValues.forEach((_, iv) -> {
                        if (iv != null) {
                            CacheHandler.putCachedValue(
                                    exposed,
                                    table,
                                    id,
                                    (dev.blitical.jigsawDB.entry.Field<T, Object>) iv.field(),
                                    iv.value()
                            );
                        }
                    });

                    return new Entry<>(exposed, table, id, false, null);
                });
    }

    public void breakDatabaseFromCache() {
        cachedMap.breakDatabase(uuid);
    }

    public <T extends Table<T, ?>> void breakTableFromCache(
            Class<T> clazz
    ) {
        @SuppressWarnings("unchecked")
        Table<T, ?> table = (Table<T, ?>) tables.get(clazz);
        if (table == null)
            throw new TableNotInitializedException(clazz.getSimpleName());

        cachedMap.breakTable(uuid, table);
    }

    public <T extends Table<T, P>, P> void breakEntryFromCache(
            Class<T> clazz,
            P primaryKey
    ) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);
        if (table == null)
            throw new TableNotInitializedException(clazz.getSimpleName());

        cachedMap.breakEntry(uuid, table, primaryKey);
    }

    public <T extends Table<T, P>, P, F extends dev.blitical.jigsawDB.entry.Field<T, V>, V> void breakFieldFromCache(
            Class<T> clazz,
            P primaryKey,
            dev.blitical.jigsawDB.entry.Field<T, V> field
    ) {
        @SuppressWarnings("unchecked")
        Table<T, P> table = (Table<T, P>) tables.get(clazz);
        if (table == null)
            throw new TableNotInitializedException(clazz.getSimpleName());

        cachedMap.breakValue(uuid, table, primaryKey, field);
    }
}

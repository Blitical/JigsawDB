package dev.blitical.jigsawDB.table;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.exceptions.compile.NoPrimaryColumnException;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Table<T extends Table<T, P>, P> {

    private final GeneratedTable<?, T> generatedTable;
    private final Set<Field<T, ?>> fields;
    private final Map<String, Field<T, ?>> fieldsByName;
    private final Map<String, java.lang.reflect.Field> reflectFieldsByColumn = new ConcurrentHashMap<>();
    private volatile TableConfig.Exposed<T> config;
    private volatile Field<T, P> primaryColumn;
    private volatile String primaryColumnName;

    public Table() {
        this.generatedTable = GeneratedTable.newInstance(this);
        this.fields = retrieveAllFields();
        this.fieldsByName = mapFieldsByName(fields);
    }

    public GeneratedTable<?, T> getGeneratedTable() {
        return generatedTable;
    }

    public Set<Field<T, ?>> retrieveAllFields() {
        java.lang.reflect.Field[] fields = generatedTable.getSelf().getClass().getDeclaredFields();
        Set<Field<T, ?>> result = new HashSet<>();

        for (var field : fields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (!Field.class.isAssignableFrom(field.getType())) continue;

            field.setAccessible(true);
            try {
                @SuppressWarnings("unchecked")
                Field<T, ?> fieldInstance = (Field<T, ?>) field.get(null);
                result.add(fieldInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }
        return Set.copyOf(result);
    }

    public @Nullable java.lang.reflect.Field retrieveColumnReflectField(Field<T, ?> field) {
        return reflectFieldsByColumn.computeIfAbsent(field.name(), this::findReflectFieldByColumnName);
    }

    private @Nullable java.lang.reflect.Field findReflectFieldByColumnName(String columnName) {
        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();
        for (var f : fields) {
            f.setAccessible(true);
            Column column = f.getAnnotation(Column.class);
            if (column == null) continue;

            if (column.value().equals(columnName))
                return f;
        }
        return null;
    }

    public final Field<T, ?> getFieldByName(String name) {
        return fieldsByName.get(name);
    }

    public Set<Field<T, ?>> getAllFields() {
        return fields;
    }

    protected void configure(TableConfig<T> config) {
    }

    public final String getTableName() {
        String modifiedName = getConfig().name();
        if (modifiedName != null) {
            return modifiedName;
        }

        String name = this.getClass().getSimpleName();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                result.append(Character.toLowerCase(c));
                continue;
            }
            if (Character.isUpperCase(c))
                result.append("_").append(Character.toLowerCase(c));
            else
                result.append(c);
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    public final Field<T, P> getPrimaryColumn() {
        Field<T, P> cachedPrimaryColumn = primaryColumn;
        if (cachedPrimaryColumn != null) {
            return cachedPrimaryColumn;
        }

        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();

        String name = null;
        for (var field : fields) {
            if (field.getAnnotation(PrimaryColumn.class) == null)
                continue;

            Column annotation = field.getAnnotation(Column.class);
            if (annotation == null)
                continue;

            name = annotation.value();
        }

        Field<T, ?> field = getFieldByName(name);
        if (field != null) {
            Field<T, P> resolved = (Field<T, P>) field;
            primaryColumn = resolved;
            return resolved;
        }

        throw new NoPrimaryColumnException(getTableName());
    }

    public final String getPrimaryColumnName() {
        String cachedPrimaryColumnName = primaryColumnName;
        if (cachedPrimaryColumnName != null) {
            return cachedPrimaryColumnName;
        }

        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();

        for (var field : fields) {
            if (field.getAnnotation(PrimaryColumn.class) == null)
                continue;

            Column annotation = field.getAnnotation(Column.class);
            if (annotation == null)
                continue;

            primaryColumnName = annotation.value();
            return primaryColumnName;
        }

        throw new NoPrimaryColumnException(getTableName());
    }

    public final DefinedColumnConfig<?> getFieldConfig(Field<T, ?> field) {
        var columns = getConfig().columns();
        ColumnConfig<?> config = columns.get(field);
        return config == null ? null : config.asDefinedConfig();
    }

    public final TableConfig.Exposed<T> getConfig() {
        TableConfig.Exposed<T> cachedConfig = config;
        if (cachedConfig != null) {
            return cachedConfig;
        }

        TableConfig<T> config = new TableConfig<>(this);
        configure(config);
        TableConfig.Exposed<T> exposed = config.getExposed();
        this.config = exposed;
        return exposed;
    }

    private Map<String, Field<T, ?>> mapFieldsByName(Set<Field<T, ?>> fields) {
        Map<String, Field<T, ?>> mappedFields = new ConcurrentHashMap<>();
        for (Field<T, ?> field : fields) {
            mappedFields.put(field.name(), field);
        }
        return Map.copyOf(mappedFields);
    }
}

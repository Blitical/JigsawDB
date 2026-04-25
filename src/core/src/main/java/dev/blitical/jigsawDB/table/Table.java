package dev.blitical.jigsawDB.table;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.exceptions.compile.NoPrimaryColumnException;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public abstract class Table<T extends Table<T, P>, P> {

    private final GeneratedTable<?, T> generatedTable;
    private final Set<Field<T, ?>> fields;

    public Table() {
        this.generatedTable = GeneratedTable.newInstance(this);
        this.fields = retrieveAllFields();
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
        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();
        Set<Field<T, ?>> result = new HashSet<>();

        for (var f : fields) {
            f.setAccessible(true);
            Column column = f.getAnnotation(Column.class);
            if (column == null) continue;

            if (column.value().equals(field.name()))
                return f;
        }
        return null;
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

        for (var field : getAllFields()) {
            if (field.name().equals(name)) {
                return (Field<T, P>) field;
            }
        }

        throw new NoPrimaryColumnException(getTableName());
    }

    public final String getPrimaryColumnName() {
        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();

        for (var field : fields) {
            if (field.getAnnotation(PrimaryColumn.class) == null)
                continue;

            Column annotation = field.getAnnotation(Column.class);
            if (annotation == null)
                continue;

            return annotation.value();
        }

        throw new NoPrimaryColumnException(getTableName());
    }

    public final DefinedColumnConfig<?> getFieldConfig(Field<T, ?> field) {
        var columns = getConfig().columns();
        return columns.get(field).asDefinedConfig();
    }

    public final TableConfig.Exposed<T> getConfig() {
        TableConfig<T> config = new TableConfig<>(this);
        configure(config);
        return config.getExposed();
    }
}

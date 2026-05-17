package dev.blitical.jigsawDB.drivers.types;

import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.encoder.ParseType;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.table.DefinedColumnConfig;
import dev.blitical.jigsawDB.table.Table;

public class TypeDefinitionResolver {
    public static <T extends Table<T, ?>> TypeDefinition resolve(Table<T, ?> table, String columnName) {
        Field<T, ?> field = table.getFieldByName(columnName);
        assert field != null; // Should NEVER happen (hopefully)

        DefinedColumnConfig<?> config = table.getFieldConfig(field);
        if (config == null || config.typeDefinition() == null) {
            return fallback(table, field);
        }
        return config.typeDefinition();
    }

    public static <T extends Table<T, ?>> TypeDefinition fallback(Table<T, ?> table, Field<T, ?> field) {
        java.lang.reflect.Field reflect = table.retrieveColumnReflectField(field);
        ParseType type = Encoder.resolveParseType(reflect);
        return switch (type) {
            case JSON -> ColumnTypes.json();
            case UUID_STRING -> ColumnTypes.uuid();
            case ENUM_STRING -> ColumnTypes.tinyText();
            case ENUM_ORDINAL -> ColumnTypes.smallint();
            case JAVA_SERIALIZED -> ColumnTypes.mediumBlob();
            case TEMPORAL_ISO -> ColumnTypes.tinyText();
            case TEMPORAL_EPOCH -> ColumnTypes.bigint();
            case BINARY -> ColumnTypes.mediumBlob();
            case INTEGER -> {
                Class<?> raw = field.getTypeToken().getRawType();
                if (raw.equals(Integer.class))
                    yield ColumnTypes.integer();
                if (raw.equals(Long.class))
                    yield ColumnTypes.bigint();
                if (raw.equals(Short.class))
                    yield ColumnTypes.smallint();
                yield ColumnTypes.integer();
            }
            case REAL -> {
                Class<?> raw = field.getTypeToken().getRawType();
                if (raw.equals(Float.class))
                    yield ColumnTypes.floatType();
                if (raw.equals(Double.class))
                    yield ColumnTypes.doubleType();
                yield ColumnTypes.doubleType();
            }
            case STRING -> ColumnTypes.text();
            case BLOB -> ColumnTypes.longBlob();
            case null -> throw new IllegalStateException("ParseType was null");
        };
    }
}

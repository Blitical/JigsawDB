package dev.blitical.jigsawDB.encoder;

import dev.blitical.jigsawDB.annotations.Parse;
import dev.blitical.jigsawDB.encoder.encoderTypes.*;
import dev.blitical.jigsawDB.table.Table;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.UUID;

import static dev.blitical.jigsawDB.encoder.ParseType.PREDEFINED_TYPES;

public final class Encoder {

    public static ParseType resolveParseType(Field field) {
        AnnotatedType type = field.getAnnotatedType();

        if (type.getAnnotation(Parse.class) != null) {
            return type.getAnnotation(Parse.class).value();
        }

        if (field.getAnnotation(Parse.class) != null) {
            return field.getAnnotation(Parse.class).value();
        }

        Class<?> typeClass = extractRawType(type);
        Parse typeAnnotation = typeClass.getDeclaredAnnotation(Parse.class);
        if (typeAnnotation != null) {
            return typeAnnotation.value();
        }

        if (typeClass.isEnum()) {
            return ParseType.ENUM_STRING;
        }

        if (typeClass.equals(UUID.class)) {
            return ParseType.UUID_STRING;
        }

        for (Map.Entry<ParseType, Class<?>[]> entry : PREDEFINED_TYPES.entrySet()) {
            for (Class<?> clazz : entry.getValue()) {
                if (clazz.isAssignableFrom(typeClass))
                    return entry.getKey();
            }
        }

        return ParseType.JSON;
    }

    private static Class<?> extractRawType(AnnotatedType type) {
        if (type.getType() instanceof Class<?> clazz) {
            return clazz;
        }

        if (type.getType() instanceof ParameterizedType pt) {
            return (Class<?>) pt.getRawType();
        }

        return Object.class;
    }

    public static EncodedType resolveEncodedType(Field field) {
        return resolveParseType(field).type;
    }

    public record EncodedObject(
            EncodedType SQLType,
            Object encoded
    ) {
    }

    public static <
            T extends Table<T, ?>, V,
            F extends dev.blitical.jigsawDB.entry.Field<T, V>
            >
    EncodedObject encode(V value, Table<T, ?> table, dev.blitical.jigsawDB.entry.Field<T, V> f) {
        java.lang.reflect.Field field = table.retrieveColumnReflectField(f);
        if (field == null) return null;

        ParseType type = resolveParseType(field);

        Object encoded = switch (type) {
            case STRING, INTEGER, REAL, BLOB -> value;
            case JSON -> JsonEncoder.encode(value);
            case JAVA_SERIALIZED -> JavaSerializedEncoder.encode(value);
            case ENUM_STRING -> EnumStringEncoder.encode(value);
            case ENUM_ORDINAL -> EnumOrdinalEncoder.encode(value);
            case UUID_STRING -> UUIDStringEncoder.encode(value);
            case TEMPORAL_EPOCH -> TemporalEpochEncoder.encode(value);
            case TEMPORAL_ISO -> TemporalIsoEncoder.encode(value);
        };

        return new EncodedObject(type.type, encoded);
    }

    public static <
            T extends Table<T, ?>, V,
            F extends dev.blitical.jigsawDB.entry.Field<T, V>
            >
    V decode(Object value, Table<T, ?> table, dev.blitical.jigsawDB.entry.Field<T, V> f) {
        java.lang.reflect.Field field = table.retrieveColumnReflectField(f);
        if (field == null || value == null) return null;

        AnnotatedType at = field.getAnnotatedType();
        ParseType type = resolveParseType(field);

        @SuppressWarnings("unchecked")
        V decoded = switch (type) {
            case STRING, BLOB -> (V) value;
            case INTEGER -> {
                Class<?> raw = f.getTypeToken().getRawType();

                if (raw.equals(Integer.class))
                    yield (V) Integer.valueOf(((Number) value).intValue());
                if (raw.equals(Long.class))
                    yield (V) Long.valueOf(((Number) value).longValue());
                if (raw.equals(Short.class))
                    yield (V) Short.valueOf(((Number) value).shortValue());
                yield (V) value;
            }
            case REAL -> {
                Class<?> raw = f.getTypeToken().getRawType();
                if (raw.equals(Float.class))
                    yield (V) Float.valueOf(((Number) value).floatValue());
                if (raw.equals(Double.class))
                    yield (V) Double.valueOf(((Number) value).doubleValue());
                yield (V) value;
            }
            case JSON -> JsonEncoder.decode((String) value, field);
            case JAVA_SERIALIZED -> JavaSerializedEncoder.decode(value, field);
            case ENUM_STRING -> EnumStringEncoder.decode((String) value, field);
            case ENUM_ORDINAL -> EnumOrdinalEncoder.decode(value, field);
            case UUID_STRING -> UUIDStringEncoder.decode((String) value, field);
            case TEMPORAL_EPOCH -> TemporalEpochEncoder.decode(value, field);
            case TEMPORAL_ISO -> TemporalIsoEncoder.decode(value, field);
        };

        return decoded;
    }

    public record CheckContext(
            TypeMirror type,
            Element e,
            ProcessingEnvironment env
    ) {
    }

    public record CheckResult(
            boolean passed,
            String customError
    ) {
        public static final CheckResult PASS = new CheckResult(true, null);
        public static final CheckResult FAIL = new CheckResult(false, null);
    }
}

package dev.blitical.jigsawDB.encoder.encoderTypes;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.encoder.ParseType;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalDecodeException;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class BinaryEncoder {
    public static Object encode(Object value) {
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static <T> T decode(Object value, Field field) {
        try {
            if (!(value instanceof byte[] bytes)) {
                throw new IllegalArgumentException("Expected byte[]");
            }
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            Class<?> type = field.getType();

            if (type == String.class)
                return (T) decoded;
            if (type == Character.class)
                return (T) decoded;

            if (type == Integer.class || type == int.class)
                return (T) Integer.valueOf(decoded);
            if (type == Long.class || type == long.class)
                return (T) Long.valueOf(decoded);
            if (type == Short.class || type == short.class)
                return (T) Short.valueOf(decoded);

            if (type == Float.class || type == float.class)
                return (T) Float.valueOf(decoded);
            if (type == Double.class || type == double.class)
                return (T) Double.valueOf(decoded);

            if (type == Boolean.class || type == boolean.class)
                return (T) Boolean.valueOf(decoded);
            if (type.isEnum())
                return (T) Enum.valueOf((Class<Enum>) type, decoded);

            throw new IllegalArgumentException("Unsupported decode type: " + type.getName());
        } catch (Exception e) {
            Column column = field.getAnnotation(Column.class);
            throw new IllegalDecodeException(
                    Objects.requireNonNullElse(column.value(), "<unknown>"),
                    field.getAnnotatedType().toString()
            );
        }
    }

    public static Encoder.CheckResult check(Encoder.CheckContext context) {
        String type = context.type().toString();

        for (var entry : ParseType.PREDEFINED_TYPES.entrySet()) {
            for (Class<?> clazz : entry.getValue()) {
                String name = getName(clazz);
                if (name.equals(type)) {
                    return Encoder.CheckResult.PASS;
                }
            }
        }

        return new Encoder.CheckResult(
                false,
                String.format("Type '%s' cannot be parsed through a Binary encoder (Currently only Strings can)", type)
        );
    }

    private static String getName(Class<?> clazz) {
        if (!clazz.isArray()) {
            return clazz.getName();
        }

        int depth = 0;
        while (clazz.isArray()) {
            depth++;
            clazz = clazz.getComponentType();
        }

        return clazz.getName() + "[]".repeat(depth);
    }
}

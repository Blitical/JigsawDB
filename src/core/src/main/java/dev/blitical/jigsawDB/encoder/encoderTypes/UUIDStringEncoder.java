package dev.blitical.jigsawDB.encoder.encoderTypes;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalDecodeException;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.UUID;

public class UUIDStringEncoder {

    public static String encode(Object value) {
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T decode(String value, Field field) {
        try {
            return (T) UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            Column column = field.getAnnotation(Column.class);
            throw new IllegalDecodeException(
                    Objects.requireNonNullElse(column.value(), "<unknown>"),
                    field.getAnnotatedType().toString()
            );
        }
    }

    public static Encoder.CheckResult check(Encoder.CheckContext context) {
        String type = context.type().toString();

        if (type.equals("java.util.UUID")) {
            return Encoder.CheckResult.PASS;
        }

        return new Encoder.CheckResult(
                false,
                String.format("Type '%s' cannot be parsed through a UUID encoder", type)
        );
    }
}

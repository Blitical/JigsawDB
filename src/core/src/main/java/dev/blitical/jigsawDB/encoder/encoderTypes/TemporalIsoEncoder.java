package dev.blitical.jigsawDB.encoder.encoderTypes;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalDecodeException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

public final class TemporalIsoEncoder {

    public static String encode(Object value) {
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T decode(Object value, Field field) {
        try {
            Class<?> raw = (Class<?>) field.getAnnotatedType().getType();
            String text = (String) value;

            if (raw.equals(Instant.class)) {
                return (T) Instant.parse(text);
            }

            if (raw.equals(LocalDateTime.class)) {
                return (T) LocalDateTime.parse(text);
            }

            if (raw.equals(OffsetDateTime.class)) {
                return (T) OffsetDateTime.parse(text);
            }

            throw new IllegalArgumentException();
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
        Set<String> allowed = Set.of(
                "java.time.LocalDateTime",
                "java.time.Instant",
                "java.time.OffsetDateTime"
        );

        if (allowed.contains(type)) {
            return Encoder.CheckResult.PASS;
        }

        return new Encoder.CheckResult(
                false,
                String.format("Type '%s' cannot be parsed through a Temporal encoder", type)
        );
    }
}

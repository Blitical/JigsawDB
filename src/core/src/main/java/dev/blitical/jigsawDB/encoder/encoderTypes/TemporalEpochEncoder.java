package dev.blitical.jigsawDB.encoder.encoderTypes;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalDecodeException;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalEncodeException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

public final class TemporalEpochEncoder {

    public static long encode(Object value) {
        if (value instanceof Instant i) {
            return i.toEpochMilli();
        }

        if (value instanceof LocalDateTime ldt) {
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        }

        if (value instanceof OffsetDateTime ldt) {
            return ldt.toInstant().toEpochMilli();
        }

        throw new IllegalEncodeException(value.toString(), "Temporal");
    }

    @SuppressWarnings("unchecked")
    public static <T> T decode(Object value, Field field) {
        try {
            long epoch = ((Number) value).longValue();
            Class<?> raw = (Class<?>) field.getAnnotatedType().getType();

            if (raw == Instant.class) {
                return (T) Instant.ofEpochMilli(epoch);
            }

            if (raw == LocalDateTime.class) {
                return (T) LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(epoch),
                        ZoneOffset.UTC
                );
            }

            if (raw == OffsetDateTime.class) {
                return (T) OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli(epoch),
                        ZoneOffset.UTC
                );
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

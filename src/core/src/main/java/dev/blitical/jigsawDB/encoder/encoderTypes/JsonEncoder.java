package dev.blitical.jigsawDB.encoder.encoderTypes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.encoder.Encoder.CheckContext;
import dev.blitical.jigsawDB.encoder.Encoder.CheckResult;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalDecodeException;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Objects;

public final class JsonEncoder {
    private static final Gson GSON = new Gson();

    public static String encode(Object value) {
        Type gsonType = value.getClass();
        return GSON.toJsonTree(value, gsonType).toString();
    }

    public static <T> T decode(String value, Field field) {
        try {
            return GSON.fromJson(value, field.getAnnotatedType().getType());
        } catch (JsonSyntaxException e) {
            Column column = field.getAnnotation(Column.class);
            throw new IllegalDecodeException(
                    Objects.requireNonNullElse(column.value(), "<unknown>"),
                    field.getAnnotatedType().toString()
            );
        }
    }

    public static CheckResult check(CheckContext context) {
        String type = context.type().toString();

        if (type.equals("java.io.InputStream") ||
                type.equals("java.io.OutputStream") ||
                type.equals("java.io.Reader")
        ) {
            return new CheckResult(
                    false,
                    String.format("Cannot parse '%s' to JSON", type)
            );
        }

        return CheckResult.PASS;
    }
}

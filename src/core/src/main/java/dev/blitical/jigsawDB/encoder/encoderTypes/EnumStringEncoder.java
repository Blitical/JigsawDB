package dev.blitical.jigsawDB.encoder.encoderTypes;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalDecodeException;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import java.lang.reflect.Field;
import java.util.Objects;

public final class EnumStringEncoder {

    public static String encode(Object value) {
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T decode(String value, Field field) {
        try {
            Class<?> raw = (Class<?>) field.getAnnotatedType().getType();
            Object[] constants = raw.getEnumConstants();

            for (Object constant : constants) {
                if (constant.toString().equals(value))
                    return (T) constant;
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

        if (context.e().asType() instanceof DeclaredType declaredType) {
            if (declaredType.asElement().getKind().equals(ElementKind.ENUM)) {
                return Encoder.CheckResult.PASS;
            }
        }

        return new Encoder.CheckResult(
                false,
                String.format("Type '%s' is not an enum", type)
        );
    }
}

package dev.blitical.jigsawDB.encoder.encoderTypes;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.encoder.Encoder.CheckContext;
import dev.blitical.jigsawDB.encoder.Encoder.CheckResult;
import dev.blitical.jigsawDB.encoder.ParseType;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalDecodeException;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalEncodeException;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Objects;

@SuppressWarnings("unchecked")
public final class JavaSerializedEncoder {

    public static Object encode(Object value) throws IllegalEncodeException {
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)
        ) {
            oos.writeObject(value);
            return baos.toByteArray();
        } catch (IOException e) {
            String val = value.toString();
            if (val.length() > 100) {
                val = val.substring(0, 100) + "...";
            }
            throw new IllegalEncodeException(val, ParseType.JAVA_SERIALIZED.toString());
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    public static <T> T decode(Object value, Field field) throws IllegalEncodeException {
        if (value == null)
            return null;
        try (
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream((byte[]) value))
        ) {
            return (T) ois.readObject();

        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            Column column = field.getAnnotation(Column.class);
            throw new IllegalDecodeException(
                    Objects.requireNonNullElse(column.value(), "<unknown>"),
                    field.getAnnotatedType().toString()
            );
        }
    }

    public static CheckResult check(CheckContext context) {
        TypeElement serializableElement = context.env().getElementUtils()
                .getTypeElement("java.io.Serializable");
        TypeMirror serializableType = serializableElement.asType();
        Types typeUtils = context.env().getTypeUtils();

        boolean implementsSerializable = typeUtils.isAssignable(context.type(), serializableType);

        if (!implementsSerializable) {
            return new CheckResult(
                    false,
                    "It must implement 'Serializable'"
            );
        }

        return CheckResult.PASS;
    }
}

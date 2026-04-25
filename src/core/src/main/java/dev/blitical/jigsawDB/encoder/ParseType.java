package dev.blitical.jigsawDB.encoder;

import java.util.Map;

public enum ParseType {
    JSON(EncodedType.STRING),
    UUID_STRING(EncodedType.STRING),
    ENUM_STRING(EncodedType.STRING),
    ENUM_ORDINAL(EncodedType.INTEGER),
    JAVA_SERIALIZED(EncodedType.BLOB),
    TEMPORAL_ISO(EncodedType.STRING),
    TEMPORAL_EPOCH(EncodedType.INTEGER),

    INTEGER(EncodedType.INTEGER),
    REAL(EncodedType.REAL),
    STRING(EncodedType.STRING),
    BLOB(EncodedType.BLOB);

    public final EncodedType type;

    ParseType(EncodedType type) {
        this.type = type;
    }

    public static final Map<ParseType, Class<?>[]> PREDEFINED_TYPES = Map.of(
            ParseType.INTEGER, new Class<?>[]{
                    int.class, Integer.class,
                    long.class, Long.class,
                    short.class, Short.class,
                    byte.class, Byte.class,
                    boolean.class, Boolean.class
            },
            ParseType.REAL, new Class<?>[]{
                    float.class, Float.class,
                    double.class, Double.class
            },
            ParseType.STRING, new Class<?>[]{
                    String.class,
                    char.class, Character.class
            },
            ParseType.BLOB, new Class<?>[]{
                    byte[].class, Byte[].class
            }
    );
}

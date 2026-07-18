package dev.blitical.jigsawDB.drivers.types;

import dev.blitical.jigsawDB.drivers.DriverType;
import dev.blitical.jigsawDB.drivers.types.definition.*;
import org.jetbrains.annotations.Range;

public class ColumnTypes {
    private static final DriverType[] ALL = new DriverType[]{
            DriverType.SQLite, DriverType.MySQL, DriverType.MariaDB, DriverType.PostgreSQL
    };
    private static final DriverType[] MY_SQL_LIKE = new DriverType[]{
            DriverType.MySQL, DriverType.MariaDB
    };

    private static final PrimaryIntegerTypeDefinition INTEGER =
            new PrimaryIntegerTypeDefinition(TypeSpec.INTEGER, null, null, null, ALL);
    private static final PrimaryIntegerTypeDefinition TINYINT =
            new PrimaryIntegerTypeDefinition(TypeSpec.TINYINT, null, null, null, ALL);
    private static final PrimaryIntegerTypeDefinition SMALLINT =
            new PrimaryIntegerTypeDefinition(TypeSpec.SMALLINT, null, null, null, ALL);
    private static final PrimaryIntegerTypeDefinition MEDIUMINT =
            new PrimaryIntegerTypeDefinition(TypeSpec.MEDIUMINT, null, null, null, ALL);
    private static final PrimaryIntegerTypeDefinition BIGINT =
            new PrimaryIntegerTypeDefinition(TypeSpec.BIGINT, null, null, null, ALL);
    private static final NumberTypeDefinition FLOAT =
            new NumberTypeDefinition(TypeSpec.FLOAT, null, null, null, ALL);
    private static final NumberTypeDefinition DOUBLE =
            new NumberTypeDefinition(TypeSpec.DOUBLE, null, null, null, ALL);
    private static final GenericTypeDefinition TINYTEXT =
            new GenericTypeDefinition(TypeSpec.TINYTEXT, null, null, null, MY_SQL_LIKE);
    private static final GenericTypeDefinition TEXT =
            new GenericTypeDefinition(TypeSpec.TEXT, null, null, null, ALL);
    private static final GenericTypeDefinition MEDIUMTEXT =
            new GenericTypeDefinition(TypeSpec.MEDIUMTEXT, null, null, null, MY_SQL_LIKE);
    private static final GenericTypeDefinition LONGTEXT =
            new GenericTypeDefinition(TypeSpec.LONGTEXT, null, null, null, MY_SQL_LIKE);
    private static final BinaryTypeDefinition BLOB =
            new BinaryTypeDefinition(TypeSpec.BLOB, null, null, null, ALL);
    private static final BinaryTypeDefinition TINYBLOB =
            new BinaryTypeDefinition(TypeSpec.TINYBLOB, null, null, null, MY_SQL_LIKE);
    private static final BinaryTypeDefinition MEDIUMBLOB =
            new BinaryTypeDefinition(TypeSpec.MEDIUMBLOB, null, null, null, MY_SQL_LIKE);
    private static final BinaryTypeDefinition LONGBLOB =
            new BinaryTypeDefinition(TypeSpec.LONGBLOB, null, null, null, MY_SQL_LIKE);
    private static final GenericTypeDefinition BOOLEAN =
            new GenericTypeDefinition(TypeSpec.BOOLEAN, null, null, null, ALL);
    private static final BinaryTypeDefinition BIT =
            new BinaryTypeDefinition(TypeSpec.BIT, null, null, null, ALL);
    private static final GenericTypeDefinition JSON =
            new GenericTypeDefinition(TypeSpec.JSON, null, null, null, ALL);
    private static final GenericTypeDefinition UUID =
            new GenericTypeDefinition(TypeSpec.UUID, null, null, null, ALL);

    /*
     * Integer types
     */

    public static PrimaryIntegerTypeDefinition integer() {
        return INTEGER;
    }

    public static PrimaryIntegerTypeDefinition tinyint() {
        return TINYINT;
    }

    public static PrimaryIntegerTypeDefinition smallint() {
        return SMALLINT;
    }

    public static PrimaryIntegerTypeDefinition mediumint() {
        return MEDIUMINT;
    }

    public static PrimaryIntegerTypeDefinition bigint() {
        return BIGINT;
    }

    /*
     * Decimal / numeric
     */

    public static NumberTypeDefinition decimal(int precision, int scale) {
        return new NumberTypeDefinition(TypeSpec.DECIMAL, null, precision, scale, ALL);
    }

    public static NumberTypeDefinition numeric(int precision, int scale) {
        return new NumberTypeDefinition(TypeSpec.NUMERIC, null, precision, scale, ALL);
    }

    /*
     * Floating point
     */

    public static NumberTypeDefinition floatType() {
        return FLOAT;
    }

    public static NumberTypeDefinition doubleType() {
        return DOUBLE;
    }

    /*
     * Character types
     */

    public static PrimaryGenericTypeDefinition charType(@Range(from = 1, to = 255) int length) {
        return new PrimaryGenericTypeDefinition(TypeSpec.CHAR, length, null, null, ALL);
    }

    public static PrimaryGenericTypeDefinition varchar(@Range(from = 1, to = 16383) int length) {
        return new PrimaryGenericTypeDefinition(TypeSpec.VARCHAR, length, null, null, ALL);
    }

    /*
     * Text types
     */

    public static GenericTypeDefinition tinyText() {
        return TINYTEXT;
    }

    public static GenericTypeDefinition text() {
        return TEXT;
    }

    public static GenericTypeDefinition mediumText() {
        return MEDIUMTEXT;
    }

    public static GenericTypeDefinition longText() {
        return LONGTEXT;
    }

    /*
     * Blob types
     */

    public static BinaryTypeDefinition blob() {
        return BLOB;
    }

    public static BinaryTypeDefinition tinyBlob() {
        return TINYBLOB;
    }

    public static BinaryTypeDefinition mediumBlob() {
        return MEDIUMBLOB;
    }

    public static BinaryTypeDefinition longBlob() {
        return LONGBLOB;
    }

    /*
     * Binary
     */

    public static BinaryTypeDefinition binary(@Range(from = 1, to = Integer.MAX_VALUE) int length) {
        return new BinaryTypeDefinition(TypeSpec.BINARY, length, null, null, ALL);
    }

    public static BinaryTypeDefinition varbinary(@Range(from = 1, to = Integer.MAX_VALUE) int length) {
        return new BinaryTypeDefinition(TypeSpec.VARBINARY, length, null, null, ALL);
    }

    /*
     * Boolean / bit
     */

    public static GenericTypeDefinition bool() {
        return BOOLEAN;
    }

    public static BinaryTypeDefinition bit() {
        return BIT;
    }

    /*
     * JSON / UUID
     */

    public static GenericTypeDefinition json() {
        return JSON;
    }

    public static GenericTypeDefinition uuid() {
        return UUID;
    }
}

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

    /*
     * Integer types
     */

    public static PrimaryIntegerTypeDefinition integer() {
        return new PrimaryIntegerTypeDefinition(TypeSpec.INTEGER, null, null, null, ALL);
    }

    public static PrimaryIntegerTypeDefinition tinyint() {
        return new PrimaryIntegerTypeDefinition(TypeSpec.TINYINT, null, null, null, ALL);
    }

    public static PrimaryIntegerTypeDefinition smallint() {
        return new PrimaryIntegerTypeDefinition(TypeSpec.SMALLINT, null, null, null, ALL);
    }

    public static PrimaryIntegerTypeDefinition mediumint() {
        return new PrimaryIntegerTypeDefinition(TypeSpec.MEDIUMINT, null, null, null, ALL);
    }

    public static PrimaryIntegerTypeDefinition bigint() {
        return new PrimaryIntegerTypeDefinition(TypeSpec.BIGINT, null, null, null, ALL);
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
        return new NumberTypeDefinition(TypeSpec.FLOAT, null, null, null, ALL);
    }

    public static NumberTypeDefinition doubleType() {
        return new NumberTypeDefinition(TypeSpec.DOUBLE, null, null, null, ALL);
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
        return new GenericTypeDefinition(TypeSpec.TINYTEXT, null, null, null, MY_SQL_LIKE);
    }

    public static GenericTypeDefinition text() {
        return new GenericTypeDefinition(TypeSpec.TEXT, null, null, null, ALL);
    }

    public static GenericTypeDefinition mediumText() {
        return new GenericTypeDefinition(TypeSpec.MEDIUMTEXT, null, null, null, MY_SQL_LIKE);
    }

    public static GenericTypeDefinition longText() {
        return new GenericTypeDefinition(TypeSpec.LONGTEXT, null, null, null, MY_SQL_LIKE);
    }

    /*
     * Blob types
     */

    public static BinaryTypeDefinition blob() {
        return new BinaryTypeDefinition(TypeSpec.BLOB, null, null, null, ALL);
    }

    public static BinaryTypeDefinition tinyBlob() {
        return new BinaryTypeDefinition(TypeSpec.TINYBLOB, null, null, null, MY_SQL_LIKE);
    }

    public static BinaryTypeDefinition mediumBlob() {
        return new BinaryTypeDefinition(TypeSpec.MEDIUMBLOB, null, null, null, MY_SQL_LIKE);
    }

    public static BinaryTypeDefinition longBlob() {
        return new BinaryTypeDefinition(TypeSpec.LONGBLOB, null, null, null, MY_SQL_LIKE);
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
        return new GenericTypeDefinition(TypeSpec.BOOLEAN, null, null, null, ALL);
    }

    public static BinaryTypeDefinition bit() {
        return new BinaryTypeDefinition(TypeSpec.BIT, null, null, null, ALL);
    }

    /*
     * JSON / UUID
     */

    public static GenericTypeDefinition json() {
        return new GenericTypeDefinition(TypeSpec.JSON, null, null, null, ALL);
    }

    public static GenericTypeDefinition uuid() {
        return new GenericTypeDefinition(TypeSpec.UUID, null, null, null, ALL);
    }
}

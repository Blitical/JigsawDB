package dev.blitical.jigsawDB.tables;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.Parse;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.encoder.ParseType;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.table.TableConfig;
import dev.blitical.jigsawDB.util.JSONClass;
import dev.blitical.jigsawDB.util.SerializableClass;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NoCachingTable extends Table<NoCachingTable, UUID> {
    @PrimaryColumn
    @Column("UUID")
    UUID UUID;

    @Column("string")
    String string;

    @Column("int")
    int integer;

    @Column("long")
    long longValue;

    @Parse(ParseType.ENUM_ORDINAL)
    @Column("enum_ordinal")
    TestEnum testEnumOrdinal;

    @Parse(ParseType.ENUM_STRING)
    @Column("enum_string")
    TestEnum testEnumString;

    @Parse(ParseType.TEMPORAL_EPOCH)
    @Column("time_epoch")
    OffsetDateTime timeEpoch;

    @Parse(ParseType.TEMPORAL_ISO)
    @Column("time_iso")
    OffsetDateTime timeISO;

    @Parse(ParseType.JAVA_SERIALIZED)
    @Column("test_serializable_class")
    SerializableClass testSerializableClass;

    @Parse(ParseType.JSON)
    @Column("test_json_class")
    JSONClass testJSONClass;

    @Column("image")
    byte[] image;

    public static enum TestEnum {
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE;
    }

    protected void configure(TableConfig<NoCachingTable> config) {
        //config.cachePolicy(CachePolicy.NONE());
    }
}

package dev.blitical.jigsawDB.tables.modified;

import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.table.TableConfig;

import java.util.UUID;

public class ModifiedTable extends Table<ModifiedTable, UUID> {
    @PrimaryColumn
    @Column("UUID")
    UUID UUID;

    @Column("string")
    String string;

    @Column("int")
    int integer;

    @Column("long")
    long longValue;

    @Override
    protected void configure(TableConfig<ModifiedTable> config) {
        config.setTableName("original_table");
        config.cachePolicy(CachePolicy.NONE());
    }
}

package dev.blitical.jigsawDB.tests;

import dev.blitical.jigsawDB.ExceptionHandler;
import dev.blitical.jigsawDB.Tests;
import dev.blitical.jigsawDB.tables.modified.ModifiedTable;
import dev.blitical.jigsawDB.tables.modified.ModifiedTableFields;
import dev.blitical.jigsawDB.tables.modified.OriginalTable;
import dev.blitical.jigsawDB.tables.modified.OriginalTableFields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;

@ExtendWith(ExceptionHandler.class)
public class TableManipulationTests {
    public static final String STRING = "Hello World";
    public static final int INTEGER = 18498192;
    public static final long LONG = 948091840212448L;

    @Test
    void tableManipulationTest() {
        Tests.createDatabases(d -> d.addTable(new OriginalTable()));
        Tests.databases.forEach(d -> {
            var entry = d.getOrCreateEntry(OriginalTable.class, Tests.TESTING_ENTRY_UUID).complete();
            entry.set(OriginalTableFields.string, STRING).complete();
            entry.set(OriginalTableFields.integer, INTEGER).complete();
            entry.set(OriginalTableFields.longValue, LONG).complete();
        });
        Tests.destroy(false);
        Tests.createDatabases(d -> d.addTable(new ModifiedTable()));
        Tests.databases.forEach(d -> {
            var entry = d.getOrCreateEntry(ModifiedTable.class, Tests.TESTING_ENTRY_UUID).complete();
            String string = entry.get(ModifiedTableFields.string).complete();
            Integer integer = entry.get(ModifiedTableFields.integer).complete();
            Long longVal = entry.get(ModifiedTableFields.longValue).complete();
            if (!STRING.equals(string)
                    || !Objects.equals(INTEGER, integer)
                    || !Objects.equals(LONG, longVal)
            ) {
                throw new IllegalStateException(String.format("""
                        Mismatched values when modifying a table:
                        - String: EXPECTED '%s'; GOT '%s'
                        - Integer: EXPECTED '%s'; GOT '%s'
                        - Long: EXPECTED '%s'; GOT '%s'""",
                        STRING, string,
                        INTEGER, integer,
                        LONG, longVal
                ));
            }
        });
        Tests.destroy();
    }
}

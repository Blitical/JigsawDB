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
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(ExceptionHandler.class)
public class TableManipulationTests {
    public static final String STRING = "Hello World";
    public static final int INTEGER = 18498192;
    public static final long LONG = 948091840212448L;

    @Test
    void tableManipulationTest() {
        Tests.createDatabases(d -> d.addTable(new OriginalTable()));
        Tests.databases.forEach(d -> {
            var entry = d.getOrCreateEntry(OriginalTable.class, Tests.TESTING_ENTRY_UUID, iv ->
                iv.set(OriginalTableFields.string, STRING)
                        .set(OriginalTableFields.integer, INTEGER)
                        .set(OriginalTableFields.longValue, LONG)
                        .build()
            ).complete();
        });
        Tests.destroy(false);
        Tests.createDatabases(d -> d.addTable(new ModifiedTable()));
        Tests.databases.forEach(d -> {
            var entry = d.getOrCreateEntry(ModifiedTable.class, Tests.TESTING_ENTRY_UUID).complete();
            final AtomicReference<String> string = new AtomicReference<>();
            final AtomicReference<Integer> integer = new AtomicReference<>();
            final AtomicReference<Long> longVal = new AtomicReference<>();
            entry.batch()
                    .get(ModifiedTableFields.string, string::set)
                    .get(ModifiedTableFields.integer, integer::set)
                    .get(ModifiedTableFields.longValue, longVal::set)
                    .fetch().complete();

            if (!STRING.equals(string.get())
                    || !Objects.equals(INTEGER, integer.get())
                    || !Objects.equals(LONG, longVal.get())
            ) {
                throw new IllegalStateException(String.format("""
                        [%s] Mismatched values when modifying a table:
                        - String: EXPECTED '%s'; GOT '%s'
                        - Integer: EXPECTED '%s'; GOT '%s'
                        - Long: EXPECTED '%s'; GOT '%s'""",
                        d.getFormattedName(),
                        STRING, string.get(),
                        INTEGER, integer.get(),
                        LONG, longVal.get()
                ));
            }
        });
        Tests.destroy();
    }
}

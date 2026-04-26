//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.config.JigsawDBConfig.Logger;
import dev.blitical.jigsawDB.config.JigsawDBConfig.Logger.LogType;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.MySQLDriver;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.tables.NoCachingTable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class Tests {
    public static final boolean DELETE_FOLDER = false;
    public static final UUID TESTING_ENTRY_UUID = UUID.randomUUID();
    public static Path testingFolder;
    public static Set<ConnectedDatabase> databases = new HashSet<>();

    public static void setUpDatabases(Table<?, ?>... tables) {
        Logger.LOG_TYPES = LogType.ALL;

        try {
            testingFolder = Files.createTempDirectory(Path.of("../"), "test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<Driver> drivers = new HashSet<>();
        //Set<Driver> drivers = new HashSet<>(Set.of(new SQLiteDriver(testingFolder.resolve("testDB.sqlite"))));
        String mySQLHost = System.getenv("MySQLHost");
        String mySQLHostUnparsed = System.getenv("MySQLPort");
        int mySQLPort = 3306;
        if (mySQLHostUnparsed != null) {
            try {
                mySQLPort = Integer.parseInt(mySQLHostUnparsed);
            } catch (NumberFormatException _) {
            }
        }

        String mySQLDatabase = System.getenv("MySQLDatabase");
        String mySQLUsername = System.getenv("MySQLUsername");
        String mySQLPassword = System.getenv("MySQLPassword");
        if (mySQLHost != null && mySQLDatabase != null && mySQLUsername != null && mySQLPassword != null) {
            drivers.add(new MySQLDriver(mySQLHost, mySQLPort, mySQLDatabase, mySQLUsername, mySQLPassword));
        } else {
            JigsawDBLogger.warn("TESTS FOR MYSQL DATABASE WILL NOT BE CARRIED OUT\nSet up these tests through the environment variables:\n- MySQLHost\n- MySQLPort (OPTIONAL: Default 3306)\n- MySQLDatabase\n- MySQLUsername\n- MySQLPassword", new Object[0]);
        }

        for (Driver driver : drivers) {
            DatabaseBuilder builder = new DatabaseBuilder(driver);

            for (Table<?, ?> table : tables) {
                builder.addTable(table);
            }

            databases.add(builder.connect().complete());
        }

    }

    public static void destroy() {
        databases.forEach(d -> {
            var e = d.getEntry(NoCachingTable.class, TESTING_ENTRY_UUID).complete();
            if (e != null)
                e.drop().complete();
        });
        databases.forEach(ConnectedDatabase::awaitShutdown);

        try (Stream<Path> paths = Files.walk(testingFolder)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(Tests::doDeletion);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JigsawDBLogger.info("[TEST] Deleted testing folder");
    }

    private static void doDeletion(File file) {
        try {
            Files.delete(file.toPath());
        } catch (Exception e) {
            JigsawDBLogger.severe(e, "Failed to delete file '%s'", file.getAbsoluteFile());
        }

    }
}

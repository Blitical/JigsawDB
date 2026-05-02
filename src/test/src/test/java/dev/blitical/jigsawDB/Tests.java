//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.config.JigsawDBConfig.Logger;
import dev.blitical.jigsawDB.config.JigsawDBConfig.Logger.LogType;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.*;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Tests {
    public static final boolean DELETE_FOLDER = true;
    public static final UUID TESTING_ENTRY_UUID = UUID.randomUUID();
    public static Path testingFolder = null;
    public static Set<ConnectedDatabase> databases = new HashSet<>();

    public static void createDatabases(Consumer<DatabaseBuilder> function) {
        Logger.LOG_TYPES = LogType.ALL;

        try {
            if (testingFolder == null) {
                testingFolder = Files.createTempDirectory(Path.of("../"), "test");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<Driver> drivers = new HashSet<>(Set.of(new SQLiteDriver(testingFolder.resolve("testDB.sqlite"))));

        createMySQL(drivers);
        createMariaDB(drivers);
        createPostgreSQL(drivers);

        for (Driver driver : drivers) {
            DatabaseBuilder builder = new DatabaseBuilder(driver);
            function.accept(builder);
            databases.add(builder.connect().complete());
        }
    }

    public static void setUpDatabases(Table<?, ?>... tables) {
        createDatabases(b -> {
            for (var table : tables) {
                b.addTable(table);
            }
        });
    }

    public static void destroy() {
        destroy(null, true);
    }

    public static void destroy(Boolean overrideDeletion, boolean dropEntries) {
        databases.forEach(d -> {
            var e = d.getEntry(NoCachingTable.class, TESTING_ENTRY_UUID).complete();
            if (e != null && dropEntries)
                e.drop().complete();
        });
        databases.forEach(ConnectedDatabase::awaitShutdown);
        databases.clear();

        if (overrideDeletion != null ? overrideDeletion : DELETE_FOLDER) {
            try (Stream<Path> paths = Files.walk(testingFolder)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(Tests::doDeletion);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JigsawDBLogger.info("[TEST] Deleted testing folder");
        }
    }

    private static void doDeletion(File file) {
        try {
            Files.delete(file.toPath());
        } catch (Exception e) {
            JigsawDBLogger.severe(e, "Failed to delete file '%s'", file.getAbsoluteFile());
        }

    }

    private static void createMySQL(Set<Driver> drivers) {
        String host = System.getenv("MySQLHost");
        String portUnparsed = System.getenv("MySQLPort");
        int port = 3306;
        if (portUnparsed != null) {
            try {
                port = Integer.parseInt(portUnparsed);
            } catch (NumberFormatException _) {
            }
        }

        String database = System.getenv("MySQLDatabase");
        String username = System.getenv("MySQLUsername");
        String password = System.getenv("MySQLPassword");
        if (host != null && database != null && username != null && password != null) {
            drivers.add(new MySQLDriver(host, port, database, username, password));
        } else {
            JigsawDBLogger.warn("""
                TESTS FOR MYSQL DATABASE WILL NOT BE CARRIED OUT
                Set up these tests through the environment variables:
                - MySQLHost
                - MySQLPort (OPTIONAL: Default 3306)
                - MySQLDatabase
                - MySQLUsername
                - MySQLPassword"""
            );
        }
    }

    private static void createMariaDB(Set<Driver> drivers) {
        String host = System.getenv("MariaDBHost");
        String portUnparsed = System.getenv("MariaDBPort");
        int port = 3306;
        if (portUnparsed != null) {
            try {
                port = Integer.parseInt(portUnparsed);
            } catch (NumberFormatException _) {
            }
        }

        String database = System.getenv("MariaDBDatabase");
        String username = System.getenv("MariaDBUsername");
        String password = System.getenv("MariaDBPassword");
        if (host != null && database != null && username != null && password != null) {
            drivers.add(new MariaDBDriver(host, port, database, username, password));
        } else {
            JigsawDBLogger.warn("""
                TESTS FOR MARIA-DB DATABASE WILL NOT BE CARRIED OUT
                Set up these tests through the environment variables:
                - MariaDBHost
                - MariaDBPort (OPTIONAL: Default 3306)
                - MariaDBDatabase
                - MariaDBUsername
                - MariaDBPassword"""
            );
        }
    }

    private static void createPostgreSQL(Set<Driver> drivers) {
        String host = System.getenv("PostgreSQLHost");
        String portUnparsed = System.getenv("PostgreSQLPort");
        int port = 5432;
        if (portUnparsed != null) {
            try {
                port = Integer.parseInt(portUnparsed);
            } catch (NumberFormatException _) {
            }
        }

        String database = System.getenv("PostgreSQLDatabase");
        String username = System.getenv("PostgreSQLUsername");
        String password = System.getenv("PostgreSQLPassword");
        if (host != null && database != null && username != null && password != null) {
            drivers.add(new PostgreSQLDriver(host, port, database, username, password));
        } else {
            JigsawDBLogger.warn("""
                TESTS FOR MARIA-DB DATABASE WILL NOT BE CARRIED OUT
                Set up these tests through the environment variables:
                - PostgreSQLHost
                - PostgreSQLPort (OPTIONAL: Default 5432)
                - PostgreSQLDatabase
                - PostgreSQLUsername
                - PostgreSQLPassword"""
            );
        }
    }
}

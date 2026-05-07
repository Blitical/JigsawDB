package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.drivers.hierarchy.MySQLikeDriver;

public class MySQLDriver extends MySQLikeDriver {
    public MySQLDriver(String host, int port, String database, String username, String password, String flags) {
        super(
                "MySQL@" + database,
                "jdbc:mysql://" + host + ":" + port + "/" + database + flags + (flags.contains("?allowMultiQueries=false") ? "" : "?allowMultiQueries=true"),
                username,
                password
        );
    }

    public MySQLDriver(String host, int port, String database, String username, String password) {
        super(
                "MySQL@" + database,
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?allowMultiQueries=true",
                username,
                password
        );
    }

    public MySQLDriver(String host, String database, String username, String password) {
        super(
                "MySQL@" + database,
                "jdbc:mysql://" + host + ":3306/" + database + "?allowMultiQueries=true",
                username,
                password
        );
    }

    @Override
    public DriverType driverType() {
        return DriverType.MySQL;
    }
}

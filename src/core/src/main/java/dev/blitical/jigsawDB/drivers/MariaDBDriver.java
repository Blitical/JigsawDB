package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.drivers.hierarchy.MySQLikeDriver;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MariaDBDriver extends MySQLikeDriver {
    public MariaDBDriver(String host, int port, String database, String username, String password, String flags) {
        super(
                "MariaDB@" + database,
                "jdbc:mariadb://" + host + ":" + port + "/" + database + flags + (flags.contains("?allowMultiQueries=false") ? "" : "?allowMultiQueries=true"),
                username,
                password
        );
    }

    public MariaDBDriver(String host, int port, String database, String username, String password) {
        super(
                "MariaDB@" + database,
                "jdbc:mariadb://" + host + ":" + port + "/" + database + "?allowMultiQueries=true",
                username,
                password
        );
    }

    public MariaDBDriver(String host, String database, String username, String password) {
        super(
                "MariaDB@" + database,
                "jdbc:mariadb://" + host + ":3306/" + database + "?allowMultiQueries=true",
                username,
                password
        );
    }

    @Override
    public DriverType driverType() {
        return DriverType.MariaDB;
    }

    @Override
    public Object getObject(ResultSet rs, String columnName) throws SQLException {
        Object raw = rs.getObject(columnName);
        if (raw instanceof Blob blob) {
            return blob.getBytes(1, (int) blob.length());
        }
        return raw;
    }
}

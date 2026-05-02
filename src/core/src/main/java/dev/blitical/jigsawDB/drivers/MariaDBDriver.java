package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.drivers.hierarchy.Base;
import dev.blitical.jigsawDB.drivers.hierarchy.MySQLikeDriver;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.selector.EntrySelector;
import dev.blitical.jigsawDB.entry.selector.condition.Condition;
import dev.blitical.jigsawDB.entry.selector.util.OrderType;
import dev.blitical.jigsawDB.table.Table;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class MariaDBDriver extends MySQLikeDriver {
    public MariaDBDriver(String host, int port, String database, String username, String password, String flags) {
        super(
                "MariaDB@" + database,
                "jdbc:mariadb://" + host + ":" + port + "/" + database + flags,
                username,
                password
        );
    }

    public MariaDBDriver(String host, int port, String database, String username, String password) {
        super(
                "MariaDB@" + database,
                "jdbc:mariadb://" + host + ":" + port + "/" + database,
                username,
                password
        );
    }

    public MariaDBDriver(String host, String database, String username, String password) {
        super(
                "MariaDB@" + database,
                "jdbc:mariadb://" + host + ":3306/" + database,
                username,
                password
        );
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

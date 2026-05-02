package dev.blitical.jigsawDB.drivers;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.hierarchy.MySQLikeDriver;
import dev.blitical.jigsawDB.drivers.misc.ExistingColumn;
import dev.blitical.jigsawDB.drivers.misc.PredefinedColumn;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.encoder.Encoder;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.FieldEntry;
import dev.blitical.jigsawDB.entry.selector.EntrySelector;
import dev.blitical.jigsawDB.entry.selector.condition.Condition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager;
import dev.blitical.jigsawDB.entry.selector.util.OrderType;
import dev.blitical.jigsawDB.table.Table;

import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MySQLDriver extends MySQLikeDriver {
    public MySQLDriver(String host, int port, String database, String username, String password, String flags) {
        super(
                "MySQL@" + database,
                "jdbc:mysql://" + host + ":" + port + "/" + database + flags,
                username,
                password
        );
    }

    public MySQLDriver(String host, int port, String database, String username, String password) {
        super(
                "MySQL@" + database,
                "jdbc:mysql://" + host + ":" + port + "/" + database,
                username,
                password
        );
    }

    public MySQLDriver(String host, String database, String username, String password) {
        super(
                "MySQL@" + database,
                "jdbc:mysql://" + host + ":3306/" + database,
                username,
                password
        );
    }
}

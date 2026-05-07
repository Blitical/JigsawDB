package dev.blitical.jigsawDB.drivers.misc;

import java.sql.SQLException;

@FunctionalInterface
public interface QueryResultFunction<T, V> {
    V apply(T t) throws SQLException;
}

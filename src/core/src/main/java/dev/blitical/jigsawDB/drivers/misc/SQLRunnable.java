package dev.blitical.jigsawDB.drivers.misc;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLRunnable {
    void run() throws SQLException;
}

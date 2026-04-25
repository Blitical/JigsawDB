package dev.blitical.jigsawDB.drivers.misc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public record QueryResult(
        PreparedStatement ps,
        ResultSet rs
) implements AutoCloseable {

    @Override
    public void close() throws SQLException {
        rs.close();
        ps.close();
    }
}

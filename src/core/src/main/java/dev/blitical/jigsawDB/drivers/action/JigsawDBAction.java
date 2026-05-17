package dev.blitical.jigsawDB.drivers.action;

import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.misc.SQLConsumer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JigsawDBAction {
    protected final Driver driver;
    protected final String SQL;
    protected final SQLConsumer<PreparedStatement> setter;
    protected final int[] statementFlags;

    protected JigsawDBAction(
            Driver driver,
            String SQL,
            SQLConsumer<PreparedStatement> setter,
            int[] statementFlags
    ) {
        this.driver = driver;
        this.SQL = SQL;
        this.setter = setter;
        this.statementFlags = statementFlags;
    }

    public static PreparedStatement prepare(
            PreparedStatement ps,
            Object... args
    ) throws SQLException {
        for (int i = 0; i < args.length; ++i) {
            ps.setObject(i + 1, args[i]);
        }
        return ps;
    }
}

package dev.blitical.jigsawDB.drivers.action;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.drivers.misc.QueryResultFunction;
import dev.blitical.jigsawDB.drivers.misc.SQLConsumer;
import org.jetbrains.annotations.ApiStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class GetAction<V> extends JigsawDBAction {
    protected final QueryResultFunction<QueryResult, V> interpreter;
    protected final boolean isInputStream;
    protected final Set<Consumer<V>> executor = new HashSet<>();

    public GetAction(
            Driver driver,
            String SQL,
            QueryResultFunction<QueryResult, V> interpreter,
            boolean isInputStream,
            Object... args
    ) {
        super(driver, (SQL.endsWith(";") ? SQL : SQL + ";"), ps -> prepare(ps, args));
        this.interpreter = interpreter;
        this.isInputStream = isInputStream;
    }

    public GetAction(
            Driver driver,
            String SQL,
            QueryResultFunction<QueryResult, V> interpreter,
            boolean isInputStream,
            SQLConsumer<PreparedStatement> setter
    ) {
        super(driver, (SQL.endsWith(";") ? SQL : SQL + ";"), setter);
        this.interpreter = interpreter;
        this.isInputStream = isInputStream;
    }

    public GetAction<V> onGet(Consumer<V> executor) {
        this.executor.add(executor);
        return this;
    }

    @ApiStatus.Internal
    public V execute(ConnectedDatabase.Exposed exposed) throws SQLException {
        if (!this.driver.equals(exposed.driver())) {
            throw new IllegalArgumentException("execute() can only ba called internally (Mismatch in drivers)");
        }

        JigsawDBLogger.sql(SQL);
        if (!isInputStream) {
            try (PreparedStatement ps = this.driver.getConnection().prepareStatement(SQL)) {
                return get(ps);
            }
        }

        // If it's an InputStream, keep the connection open
        PreparedStatement ps = this.driver.getConnection().prepareStatement(SQL);
        try {
            return get(ps);
        } catch (Exception e) {
            ps.close();
            this.driver.getConnection().close();
            throw e;
        }
    }

    @ApiStatus.Internal
    public V executeWithRuntimeException(ConnectedDatabase.Exposed exposed) {
        try {
            return execute(exposed);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private V get(PreparedStatement ps) throws SQLException {
        setter.accept(ps);
        ResultSet rs = ps.executeQuery();
        V value = interpreter.apply(new QueryResult(ps, rs));
        executor.forEach(e -> e.accept(value));
        return value;
    }
}

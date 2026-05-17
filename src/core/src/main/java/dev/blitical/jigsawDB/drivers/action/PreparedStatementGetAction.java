package dev.blitical.jigsawDB.drivers.action;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.misc.QueryResultFunction;
import dev.blitical.jigsawDB.drivers.misc.SQLConsumer;
import org.jetbrains.annotations.ApiStatus;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class PreparedStatementGetAction<V> extends JigsawDBAction {
    protected final QueryResultFunction<PreparedStatement, V> interpreter;
    protected final Set<Consumer<V>> executor = new HashSet<>();

    public PreparedStatementGetAction(
            Driver driver,
            String SQL,
            QueryResultFunction<PreparedStatement, V> interpreter,
            Object... args
    ) {
        super(driver, (SQL.endsWith(";") ? SQL : SQL + ";"), ps -> prepare(ps, args), new int[]{});
        this.interpreter = interpreter;
    }

    public PreparedStatementGetAction(
            Driver driver,
            String SQL,
            QueryResultFunction<PreparedStatement, V> interpreter,
            SQLConsumer<PreparedStatement> setter,
            int... statementFlags
    ) {
        super(driver, (SQL.endsWith(";") ? SQL : SQL + ";"), setter, statementFlags);
        this.interpreter = interpreter;
    }

    public PreparedStatementGetAction<V> onGet(Consumer<V> executor) {
        this.executor.add(executor);
        return this;
    }

    @ApiStatus.Internal
    public V execute(ConnectedDatabase.Exposed exposed) throws SQLException {
        if (!this.driver.equals(exposed.driver())) {
            throw new IllegalArgumentException("execute() can only be called internally (Mismatch in drivers)");
        }

        JigsawDBLogger.sql(SQL);
        try (PreparedStatement ps = this.driver.getConnection().prepareStatement(SQL, statementFlags)) {
            return get(ps);
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
        ps.execute();
        V value = interpreter.apply(ps);
        executor.forEach(e -> e.accept(value));
        return value;
    }
}

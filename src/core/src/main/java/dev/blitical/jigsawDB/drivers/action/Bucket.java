package dev.blitical.jigsawDB.drivers.action;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.misc.QueryResult;
import dev.blitical.jigsawDB.value.ExecutableFutureVoid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Bucket {
    private final ConnectedDatabase.Exposed exposed;
    private Driver driver;
    private final List<JigsawDBAction> actions = new ArrayList<>();

    public Bucket(ConnectedDatabase.Exposed exposed) {
        this.exposed = exposed;
        this.driver = exposed.driver();
    }

    public Bucket add(JigsawDBAction action) {
        if (!action.driver.equals(driver)) {
            throw new IllegalArgumentException("Action driver must correspond to the connected database used to create a bucket");
        }
        actions.add(action);
        return this;
    }

    public Bucket remove(JigsawDBAction action) {
        actions.remove(action);
        return this;
    }

    public ExecutableFutureVoid execute() {
        return new ExecutableFutureVoid(exposed, () -> of(actions.toArray(JigsawDBAction[]::new)).execute());
    }

    public BucketAction of(JigsawDBAction... actions) {
        driver = resolveDriver(actions);
        return new BucketAction(driver, () -> {
            Connection connection = driver.getConnection();
            connection.setAutoCommit(false);

            try {
                StringBuilder log = new StringBuilder("Bucket Batch Call:\n");
                for (JigsawDBAction action : actions) {
                    log.append("- ").append(action.SQL).append("\n");
                    try (PreparedStatement ps = connection.prepareStatement(action.SQL)) {
                        action.setter.accept(ps);
                        if (action instanceof GetAction<?> getAction) {
                            try (ResultSet rs = ps.executeQuery()) {
                                handleGetAction(rs, getAction);
                            }
                        } else if (action instanceof Action setAction) {
                            ps.executeUpdate();
                            setAction.onComplete.forEach(Runnable::run);
                        }
                    }
                }

                log.append("[BUCKET CALL] [").append(driver.formatedName()).append("]");
                JigsawDBLogger.sql(log.toString());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    private Driver resolveDriver(JigsawDBAction... actions) {
        Driver driver = exposed.driver();
        for (JigsawDBAction action : actions) {
            if (!action.driver.equals(driver)) {
                throw new IllegalArgumentException("All drivers in a Bucket must be the same");
            }
        }
        return driver;
    }

    private <V> void handleGetAction(ResultSet rs, GetAction<V> getAction) throws SQLException {
        V value = getAction.interpreter.apply(new QueryResult(null, rs));
        getAction.executor.forEach(e -> e.accept(value));
    }
}

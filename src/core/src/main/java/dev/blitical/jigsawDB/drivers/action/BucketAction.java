package dev.blitical.jigsawDB.drivers.action;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.misc.SQLRunnable;
import org.jetbrains.annotations.ApiStatus;

import java.sql.SQLException;

public class BucketAction {
    private final Driver driver;
    private final SQLRunnable runnable;

    public BucketAction(
            Driver driver,
            SQLRunnable runnable
    ) {
        this.driver = driver;
        this.runnable = runnable;
    }

    @ApiStatus.Internal
    protected void execute() throws SQLException {
        runnable.run();
    }

    @ApiStatus.Internal
    public void execute(ConnectedDatabase.Exposed exposed) throws SQLException {
        if (!this.driver.equals(exposed.driver())) {
            throw new IllegalArgumentException("execute() can only ba called internally (Mismatch in drivers)");
        }
        execute();
    }

    @ApiStatus.Internal
    public void executeWithRuntimeException(ConnectedDatabase.Exposed exposed) {
        try {
            execute(exposed);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

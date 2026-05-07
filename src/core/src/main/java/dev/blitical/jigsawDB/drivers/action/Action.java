package dev.blitical.jigsawDB.drivers.action;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.config.JigsawDBLogger;
import dev.blitical.jigsawDB.drivers.Driver;
import dev.blitical.jigsawDB.drivers.misc.SQLConsumer;
import org.jetbrains.annotations.ApiStatus;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public final class Action extends JigsawDBAction {
    protected final Set<Runnable> onComplete = new HashSet<>();

    public Action(
            Driver driver,
            String SQL,
            Object... args
    ) {
        super(driver, (SQL.endsWith(";") ? SQL : SQL + ";"), ps -> prepare(ps, args));
    }

    public Action(
            Driver driver,
            String SQL,
            SQLConsumer<PreparedStatement> setter
    ) {
        super(driver, (SQL.endsWith(";") ? SQL : SQL + ";"), setter);
    }

    public Action onComplete(Runnable onComplete) {
        this.onComplete.add(onComplete);
        return this;
    }

    @ApiStatus.Internal
    public void execute(ConnectedDatabase.Exposed exposed) throws SQLException {
        if (!this.driver.equals(exposed.driver())) {
            throw new IllegalArgumentException("execute() can only be called internally (Mismatch in drivers)");
        }

        JigsawDBLogger.sql(SQL);
        try (PreparedStatement ps = this.driver.getConnection().prepareStatement(SQL)) {
            setter.accept(ps);
            ps.execute();
        }
        onComplete.forEach(Runnable::run);
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

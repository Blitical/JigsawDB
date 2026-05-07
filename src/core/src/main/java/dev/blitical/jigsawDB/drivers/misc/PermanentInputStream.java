package dev.blitical.jigsawDB.drivers.misc;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Statement;

public class PermanentInputStream extends FilterInputStream {
    private final Statement stmt;

    public PermanentInputStream(
            InputStream in,
            Statement stmt
    ) {
        super(in);
        this.stmt = stmt;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception ignored) {
            }
        }
    }
}

package dev.blitical.jigsawDB.config;

import dev.blitical.jigsawDB.config.JigsawDBConfig.Logger;
import dev.blitical.jigsawDB.config.JigsawDBConfig.Logger.LogType;

public class JigsawDBLogger {

    public static void info(Object format, Object... args) {
        if (!contains(LogType.INFO)) return;
        Logger.INFO.accept(String.format(String.valueOf(format), args));
    }

    public static void debug(Object format, Object... args) {
        if (!contains(LogType.DEBUG)) return;
        Logger.DEBUG.accept(String.format(String.valueOf(format), args));
    }

    public static void sql(Object format, Object... args) {
        if (!contains(LogType.SQL)) return;
        Logger.SQL.accept(String.format(String.valueOf(format), args));
    }

    public static void warn(Object format, Object... args) {
        if (!contains(LogType.WARN)) return;
        Logger.WARN.accept(String.format(String.valueOf(format), args));
    }

    public static void severe(Object format, Object... args) {
        if (!contains(LogType.SEVERE)) return;
        Logger.SEVERE.accept(String.format(String.valueOf(format), args));
    }

    public static void severe(Throwable t, Object format, Object... args) {
        if (!contains(LogType.SEVERE)) return;
        Logger.SEVERE_THROWABLE.accept(String.format(String.valueOf(format), args), t);
    }

    private static boolean contains(LogType type) {
        for (var t : Logger.LOG_TYPES) {
            if (t == type) return true;
        }
        return false;
    }
}

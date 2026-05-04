package dev.blitical.jigsawDB.config;

import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class JigsawDBConfig {

    public static class ExecutableFuture {
        public record TimeoutConfig(
                Long duration,
                TimeUnit unit
        ) {
            public static TimeoutConfig NO_TIMEOUT = new TimeoutConfig(null, null);
        }

        public static TimeoutConfig TIMEOUT_CONFIG = new TimeoutConfig(5L, TimeUnit.SECONDS);
    }

    public static class Logger {

        public enum LogType {
            INFO, DEBUG, SQL, WARN, SEVERE;

            public static final LogType[] ALL = {INFO, DEBUG, SQL, WARN, SEVERE};
            public static final LogType[] NONE = {};
        }

        public static LogType[] LOG_TYPES = {LogType.INFO, LogType.WARN, LogType.SEVERE};

        private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("JigsawDB");

        public static Consumer<String> INFO = LOGGER::info;

        public static Consumer<String> DEBUG = LOGGER::debug;

        public static Consumer<String> SQL = msg -> LOGGER.debug("[SQL] {}", msg);

        public static Consumer<String> WARN = LOGGER::warn;

        public static Consumer<String> SEVERE = LOGGER::error;

        public static BiConsumer<String, Throwable> SEVERE_THROWABLE
                = (msg, t) -> {
            final int size = 15;
            if (msg.endsWith(": ")) // For blitical lol
                msg = msg.substring(0, msg.length() - 2);
            SEVERE.accept(msg + "\nStack Trace: "
                    + "=".repeat(size) + "\n"
                    + getStackTraceLines(t)
                    + "=".repeat(size + 13)
            );
        };

        private static String getStackTraceLines(Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }
}

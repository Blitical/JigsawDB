package dev.blitical.jigsawDB.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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


        public static Consumer<String> INFO = msg ->
                System.out.println(format("#defdff", "INFO", msg));

        public static Consumer<String> DEBUG = msg ->
                System.out.println(format("#3a96dd", "DEBUG", msg));

        public static Consumer<String> SQL = msg ->
                System.out.println(format("#c73085", "SQL", msg));

        public static Consumer<String> WARN = msg ->
                System.out.println(format("#ffff55", "WARN", msg));

        public static Consumer<String> SEVERE = msg ->
                System.out.println(format("#ff5555", "SEVERE", msg));

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

        private static String format(String HEX, String prefix, String message) {
            String msg = Arrays.stream(message.split("[\\n\\r]"))
                    .map(s -> hexToAnsi(HEX) + s)
                    .collect(Collectors.joining("\n"));
            return hexToAnsi("#ababab")
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + hexToAnsi(HEX) + " [" + prefix + "] " + msg + "\033[0m";
        }

        private static String hexToAnsi(String hexColor) {
            if (hexColor.startsWith("#"))
                hexColor = hexColor.substring(1);

            int r = Integer.parseInt(hexColor.substring(0, 2), 16);
            int g = Integer.parseInt(hexColor.substring(2, 4), 16);
            int b = Integer.parseInt(hexColor.substring(4, 6), 16);

            return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
        }

        private static String getStackTraceLines(Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }
}

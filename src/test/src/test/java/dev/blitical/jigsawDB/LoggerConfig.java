package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.config.JigsawDBConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LoggerConfig {
    public static void init() {
        JigsawDBConfig.Logger.INFO = msg ->
                System.out.println(format("#defdff", "INFO", msg));

        JigsawDBConfig.Logger.DEBUG = msg ->
                System.out.println(format("#3a96dd", "DEBUG", msg));

        JigsawDBConfig.Logger.SQL = msg ->
                System.out.println(format("#c73085", "SQL", msg));

        JigsawDBConfig.Logger.WARN = msg ->
                System.out.println(format("#ffff55", "WARN", msg));

        JigsawDBConfig.Logger.SEVERE = msg ->
                System.out.println(format("#ff5555", "SEVERE", msg));
    }

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
}

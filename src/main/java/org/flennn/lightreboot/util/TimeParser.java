package org.flennn.lightreboot.util;

import java.util.Locale;

public final class TimeParser {
    private TimeParser() {
    }

    public static int parseSeconds(String input) {
        if (input == null || input.trim().isEmpty()) {
            return -1;
        }

        String value = input.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("s")) {
                return Integer.parseInt(value.substring(0, value.length() - 1));
            }
            if (value.endsWith("m")) {
                return Math.multiplyExact(Integer.parseInt(value.substring(0, value.length() - 1)), 60);
            }
            if (value.endsWith("h")) {
                return Math.multiplyExact(Integer.parseInt(value.substring(0, value.length() - 1)), 3600);
            }
            return Integer.parseInt(value);
        } catch (ArithmeticException | NumberFormatException e) {
            return -1;
        }
    }

    public static String format(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int rest = seconds % 60;
        if (minutes < 60) {
            return rest == 0 ? minutes + "m" : minutes + "m " + rest + "s";
        }
        int hours = minutes / 60;
        int minuteRest = minutes % 60;
        return minuteRest == 0 ? hours + "h" : hours + "h " + minuteRest + "m";
    }
}

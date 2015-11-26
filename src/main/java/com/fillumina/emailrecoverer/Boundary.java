package com.fillumina.emailrecoverer;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class Boundary {

    /** Validates a boundary. */
    public static boolean isValid(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        final int length = line.length();
        if (length < 20 || length > 100) {
            return false;
        }
        if (!line.startsWith("--")) {
            return false;
        }
        if (line.charAt(length - 3) == '-') {
            return false;
        }
        char[] array = line.toCharArray();
        for (char c : array) {
            if (c == ' ' || c == '<' || c == '>' ||
                    c == '*' || c == '~' ||
                    c == '=' || c > 127 || c < 32) {
                return false;
            }
        }
        return true;
    }

    public static String removeCloseSimbol(final String line) {
        return isClose(line) ? line.substring(0, line.length() - 2) : line;
    }

    public static boolean isClose(String line) {
        return line.endsWith("--");
    }
}

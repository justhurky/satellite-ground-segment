package io.github.justhurky.groundsegment.common.util;

public final class LogSanitiser {

    public static final int MAX_LOGGED_LENGTH = 200;
    public static final char REPLACEMENT = '_';
    public static final String TRUNCATION_MARKER = "...";

    private LogSanitiser() {
    }

    public static String oneLine(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int length = Math.min(value.length(), MAX_LOGGED_LENGTH);
        StringBuilder sanitised = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            char character = value.charAt(index);
            sanitised.append(Character.isISOControl(character) ? REPLACEMENT : character);
        }
        if (value.length() > MAX_LOGGED_LENGTH) {
            sanitised.append(TRUNCATION_MARKER);
        }
        return sanitised.toString();
    }
}

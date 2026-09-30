package eye.on.the.money.util;

public final class LikePatterns {

    public static final char ESCAPE = '\\';

    private LikePatterns() {
    }

    public static String contains(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2).append('%');
        for (char character : value.toCharArray()) {
            if (character == '%' || character == '_' || character == ESCAPE) {
                escaped.append(ESCAPE);
            }
            escaped.append(character);
        }
        return escaped.append('%').toString();
    }
}

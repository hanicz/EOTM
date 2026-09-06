package eye.on.the.money.util;

import eye.on.the.money.exception.ValidationException;

public final class Numbers {

    private static final char NO_BREAK_SPACE = (char) 0x00A0;
    private static final char NARROW_NO_BREAK_SPACE = (char) 0x202F;
    private static final char GROUPING_SEPARATOR = '.';
    private static final char DECIMAL_SEPARATOR = ',';

    private Numbers() {
    }

    public static Double parseHungarian(String number) {
        StringBuilder normalised = new StringBuilder();
        for (char character : number.toCharArray()) {
            if (Character.isWhitespace(character) || character == NO_BREAK_SPACE
                    || character == NARROW_NO_BREAK_SPACE || character == GROUPING_SEPARATOR) {
                continue;
            }
            normalised.append(character == DECIMAL_SEPARATOR ? '.' : character);
        }
        if (normalised.isEmpty()) {
            throw new ValidationException("Missing amount");
        }
        return Double.parseDouble(normalised.toString());
    }
}

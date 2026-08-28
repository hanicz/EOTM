package eye.on.the.money.util;

import eye.on.the.money.exception.ValidationException;

import java.util.Locale;

public final class Ticker {

    private static final String SEPARATOR = ".";

    private Ticker() {
    }

    public static String normalizeShortName(String shortName) {
        return Ticker.normalize(shortName, "shortName");
    }

    public static String normalizeExchange(String exchange) {
        return Ticker.normalize(exchange, "exchange");
    }

    public static String id(String shortName, String exchange) {
        return Ticker.symbol(shortName, exchange).toLowerCase(Locale.ROOT);
    }

    public static String symbol(String shortName, String exchange) {
        return Ticker.normalizeShortName(shortName) + Ticker.SEPARATOR + Ticker.normalizeExchange(exchange);
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Security " + field + " must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}

package eye.on.the.money.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Reads a price out of an EOD real-time quote.
 *
 * <p>EOD returns the string "NA" instead of a number for every field it has no data for, which happens
 * regularly outside US market hours and for thinly traded exchanges such as BUD. Jackson maps that string to
 * 0.0 on a numeric read, so an unquoted holding used to be valued at zero. Where the last close is missing
 * the previous one is usually still there, and yesterday's price beats no price at all - {@link Price#stale()}
 * says which of the two was used so the UI can mark it. {@link Price#change()} and
 * {@link Price#changePercent()} carry the move since the previous close and are null on the same "NA" terms.
 */
public final class LiveQuote {

    private static final String CLOSE = "close";

    private static final String PREVIOUS_CLOSE = "previousClose";

    private static final String CHANGE = "change";

    private static final String CHANGE_PERCENT = "change_p";

    public record Price(BigDecimal value, boolean stale, BigDecimal change, Double changePercent) {
    }

    private LiveQuote() {
    }

    public static Optional<Price> price(JsonNode quote) {
        BigDecimal change = LiveQuote.decimal(quote, LiveQuote.CHANGE);
        Double changePercent = LiveQuote.boxed(quote, LiveQuote.CHANGE_PERCENT);

        BigDecimal close = LiveQuote.decimal(quote, LiveQuote.CLOSE);
        if (close != null) return Optional.of(new Price(close, false, change, changePercent));

        BigDecimal previousClose = LiveQuote.decimal(quote, LiveQuote.PREVIOUS_CLOSE);
        return previousClose != null
                ? Optional.of(new Price(previousClose, true, change, changePercent))
                : Optional.empty();
    }

    public static BigDecimal decimal(JsonNode quote, String field) {
        JsonNode value = quote.findValue(field);
        return value != null && value.isNumber() ? value.decimalValue() : null;
    }

    public static OptionalDouble numeric(JsonNode quote, String field) {
        JsonNode value = quote.findValue(field);
        return value != null && value.isNumber() ? OptionalDouble.of(value.doubleValue()) : OptionalDouble.empty();
    }

    public static double numericOrZero(JsonNode quote, String field) {
        return LiveQuote.numeric(quote, field).orElse(0.0);
    }

    private static Double boxed(JsonNode quote, String field) {
        JsonNode value = quote.findValue(field);
        return value != null && value.isNumber() ? value.doubleValue() : null;
    }
}

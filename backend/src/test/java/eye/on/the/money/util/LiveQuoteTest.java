package eye.on.the.money.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class LiveQuoteTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String EOD_RESPONSE = """
            [{"code":"STOCK.US","timestamp":"NA","gmtoffset":0,"open":"NA","high":"NA","low":"NA","close":"NA",
              "volume":"NA","previousClose":"NA","change":"NA","change_p":"NA"},
             {"code":"RICHTER.BUD","timestamp":"NA","gmtoffset":0,"open":"NA","high":"NA","low":"NA","close":"NA",
              "volume":"NA","previousClose":12880,"change":"NA","change_p":"NA"},
             {"code":"AMD.US","timestamp":1787603340,"gmtoffset":0,"open":468.54,"high":468.54,"low":451,
              "close":456.745,"volume":15968263,"previousClose":473.25,"change":-16.505,"change_p":-3.4876}]""";

    @Test
    void price_usesCloseWhenQuoted() {
        LiveQuote.Price price = LiveQuote.price(this.quote("AMD.US")).orElseThrow();

        assertEquals(456.745, price.value());
        assertFalse(price.stale());
    }

    @Test
    void price_fallsBackToPreviousCloseWhenCloseIsNotAvailable() {
        LiveQuote.Price price = LiveQuote.price(this.quote("RICHTER.BUD")).orElseThrow();

        assertEquals(12880.0, price.value());
        assertTrue(price.stale());
    }

    @Test
    void price_isEmptyWhenNeitherIsAvailable() {
        assertTrue(LiveQuote.price(this.quote("STOCK.US")).isEmpty());
    }

    @Test
    void price_carriesTheDailyChangeWhenQuoted() {
        LiveQuote.Price price = LiveQuote.price(this.quote("AMD.US")).orElseThrow();

        assertEquals(-16.505, price.change());
        assertEquals(-3.4876, price.changePercent());
    }

    @Test
    void price_leavesTheDailyChangeNullWhenNotAvailable() {
        LiveQuote.Price stale = LiveQuote.price(this.quote("RICHTER.BUD")).orElseThrow();

        assertNull(stale.change());
        assertNull(stale.changePercent());
    }

    @Test
    void numeric_isEmptyForMissingField() {
        assertTrue(LiveQuote.numeric(this.quote("AMD.US"), "nope").isEmpty());
    }

    @Test
    void numericOrZero_treatsNotAvailableAsZero() {
        assertEquals(0.0, LiveQuote.numericOrZero(this.quote("RICHTER.BUD"), "change_p"));
        assertEquals(-3.4876, LiveQuote.numericOrZero(this.quote("AMD.US"), "change_p"));
    }

    @Test
    void numeric_doesNotReadNotAvailableAsZeroTimestamp() {
        OptionalDouble timestamp = LiveQuote.numeric(this.quote("RICHTER.BUD"), "timestamp");

        assertTrue(timestamp.isEmpty());
    }

    private JsonNode quote(String code) {
        try {
            for (JsonNode node : LiveQuoteTest.MAPPER.readTree(LiveQuoteTest.EOD_RESPONSE)) {
                if (code.equals(node.path("code").asText())) return node;
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalArgumentException("No quote for " + code);
    }
}

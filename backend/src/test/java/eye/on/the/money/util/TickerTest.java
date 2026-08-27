package eye.on.the.money.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TickerTest {

    @Test
    public void symbolIsUpperCased() {
        Assertions.assertEquals("VWCE.XETRA", Ticker.symbol("vwce", "xetra"));
    }

    @Test
    public void symbolTrimsSurroundingWhitespace() {
        Assertions.assertEquals("VWCE.XETRA", Ticker.symbol("  vwce ", " XETRA  "));
    }

    @Test
    public void idIsLowerCased() {
        Assertions.assertEquals("vwce.xetra", Ticker.id("VWCE", "XETRA"));
    }

    @Test
    public void idKeepsExchangesApart() {
        Assertions.assertNotEquals(Ticker.id("VWCE", "XETRA"), Ticker.id("VWCE", "MI"));
    }

    @Test
    public void idPreservesDottedTickers() {
        Assertions.assertEquals("brk.b.us", Ticker.id("BRK.B", "US"));
    }

    @Test
    public void blankShortNameIsRejected() {
        Assertions.assertAll(
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> Ticker.symbol(null, "US")),
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> Ticker.symbol("  ", "US")));
    }

    @Test
    public void blankExchangeIsRejected() {
        Assertions.assertAll(
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> Ticker.symbol("CRSR", null)),
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> Ticker.symbol("CRSR", "  ")));
    }
}

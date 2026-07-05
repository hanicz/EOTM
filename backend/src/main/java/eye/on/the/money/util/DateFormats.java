package eye.on.the.money.util;

import java.time.format.DateTimeFormatter;

public final class DateFormats {

    private DateFormats() {
    }

    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}

package eye.on.the.money.dto;

import java.math.BigDecimal;

public interface CSVHelper {
    Object[] getHeaders();

    Object[] getCSVRecord();

    static String plainNumber(BigDecimal value) {
        return (value == null) ? null : value.stripTrailingZeros().toPlainString();
    }
}

package eye.on.the.money.dto;

import java.math.BigDecimal;

public interface CSVHelper {
    Object[] getHeaders();

    Object[] getCSVRecord();

    static String plainNumber(Double value) {
        return (value == null) ? null : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}

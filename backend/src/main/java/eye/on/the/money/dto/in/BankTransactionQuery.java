package eye.on.the.money.dto.in;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BankTransactionQuery(String search,
                                   BankTransactionFlag flag,
                                   Long categoryId,
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   BigDecimal minAmount,
                                   BigDecimal maxAmount) {

    public static final long UNCATEGORIZED = 0L;

    public static BankTransactionQuery none() {
        return new BankTransactionQuery(null, null, null, null, null, null, null);
    }
}

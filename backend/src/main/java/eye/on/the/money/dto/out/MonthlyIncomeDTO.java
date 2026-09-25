package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class MonthlyIncomeDTO implements CSVHelper {

    private Integer year;
    private Integer month;
    private String currencyId;
    private String source;
    private BigDecimal amount;
    private Long transactionCount;

    public BigDecimal getAmount() {
        return this.amount == null ? null : this.amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Year", "Month", "Currency", "Source", "Amount", "Transactions"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getYear(), this.getMonth(), this.getCurrencyId(), this.getSource(),
                this.getAmount(), this.getTransactionCount()};
    }
}

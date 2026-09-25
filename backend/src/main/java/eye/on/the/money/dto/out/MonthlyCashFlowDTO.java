package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.util.Numbers;
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
public class MonthlyCashFlowDTO implements CSVHelper {

    private Integer year;
    private Integer month;
    private String currencyId;
    private BigDecimal moneyIn;
    private BigDecimal moneyOut;

    public BigDecimal getMoneyIn() {
        return round(this.moneyIn);
    }

    public BigDecimal getMoneyOut() {
        return round(this.moneyOut);
    }

    public BigDecimal getNet() {
        return round(this.moneyIn.add(this.moneyOut));
    }

    /**
     * What share of the month's income was left over. Null rather than zero when nothing came in, so the UI
     * can leave the month blank instead of drawing a 0% that would read as "kept nothing" when the truth is
     * "there is nothing to divide by".
     */
    public BigDecimal getSavedPercent() {
        if (this.moneyIn == null || this.moneyIn.signum() == 0) {
            return null;
        }
        return round(Numbers.divide(this.moneyIn.add(this.moneyOut), this.moneyIn).multiply(Numbers.HUNDRED));
    }

    private static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Year", "Month", "Currency", "Money In", "Money Out", "Net", "Saved %"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getYear(), this.getMonth(), this.getCurrencyId(), this.getMoneyIn(),
                this.getMoneyOut(), this.getNet(), this.getSavedPercent()};
    }
}

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
public class YearlyCashFlowDTO implements CSVHelper {

    private Integer year;
    private String currencyId;
    private BigDecimal moneyIn;
    private BigDecimal moneyOut;
    private Integer monthsCounted;

    public static YearlyCashFlowDTO empty(Integer year, String currencyId) {
        return YearlyCashFlowDTO.builder()
                .year(year)
                .currencyId(currencyId)
                .moneyIn(BigDecimal.ZERO)
                .moneyOut(BigDecimal.ZERO)
                .monthsCounted(0)
                .build();
    }

    public void add(MonthlyCashFlowDTO month) {
        this.moneyIn = this.moneyIn.add(month.getMoneyIn());
        this.moneyOut = this.moneyOut.add(month.getMoneyOut());
        this.monthsCounted = this.monthsCounted + 1;
    }

    public BigDecimal getMoneyIn() {
        return round(this.moneyIn);
    }

    public BigDecimal getMoneyOut() {
        return round(this.moneyOut);
    }

    public BigDecimal getNet() {
        return round(this.moneyIn.add(this.moneyOut));
    }

    public BigDecimal getSavedPercent() {
        if (this.moneyIn == null || this.moneyIn.signum() == 0) {
            return null;
        }
        return round(Numbers.divide(this.moneyIn.add(this.moneyOut), this.moneyIn).multiply(Numbers.HUNDRED));
    }

    public BigDecimal getAverageMonthlyNet() {
        if (this.monthsCounted == null || this.monthsCounted == 0) {
            return null;
        }
        return round(Numbers.divide(this.moneyIn.add(this.moneyOut), BigDecimal.valueOf(this.monthsCounted)));
    }

    public BigDecimal getAverageMonthlySpending() {
        if (this.monthsCounted == null || this.monthsCounted == 0) {
            return null;
        }
        return round(Numbers.divide(this.moneyOut, BigDecimal.valueOf(this.monthsCounted)));
    }

    private static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Year", "Currency", "Months", "Money In", "Money Out", "Net", "Saved %",
                "Avg Monthly Net", "Avg Monthly Spending"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getYear(), this.getCurrencyId(), this.getMonthsCounted(), this.getMoneyIn(),
                this.getMoneyOut(), this.getNet(), this.getSavedPercent(), this.getAverageMonthlyNet(),
                this.getAverageMonthlySpending()};
    }
}

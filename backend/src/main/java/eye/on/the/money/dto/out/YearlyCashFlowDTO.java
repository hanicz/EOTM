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
public class YearlyCashFlowDTO implements CSVHelper {

    private Integer year;
    private String currencyId;
    private Double moneyIn;
    private Double moneyOut;
    private Integer monthsCounted;

    public static YearlyCashFlowDTO empty(Integer year, String currencyId) {
        return YearlyCashFlowDTO.builder()
                .year(year)
                .currencyId(currencyId)
                .moneyIn(0.0)
                .moneyOut(0.0)
                .monthsCounted(0)
                .build();
    }

    public void add(MonthlyCashFlowDTO month) {
        this.moneyIn = this.moneyIn + month.getMoneyIn();
        this.moneyOut = this.moneyOut + month.getMoneyOut();
        this.monthsCounted = this.monthsCounted + 1;
    }

    public Double getMoneyIn() {
        return round(this.moneyIn);
    }

    public Double getMoneyOut() {
        return round(this.moneyOut);
    }

    public Double getNet() {
        return round(this.moneyIn + this.moneyOut);
    }

    public Double getSavedPercent() {
        if (this.moneyIn == null || this.moneyIn == 0.0) {
            return null;
        }
        return round(((this.moneyIn + this.moneyOut) / this.moneyIn) * 100.0);
    }

    public Double getAverageMonthlyNet() {
        if (this.monthsCounted == null || this.monthsCounted == 0) {
            return null;
        }
        return round((this.moneyIn + this.moneyOut) / this.monthsCounted);
    }

    public Double getAverageMonthlySpending() {
        if (this.monthsCounted == null || this.monthsCounted == 0) {
            return null;
        }
        return round(this.moneyOut / this.monthsCounted);
    }

    private static Double round(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
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

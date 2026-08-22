package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class MonthlyCashFlowDTO implements CSVHelper {

    private Integer year;
    private Integer month;
    private String currencyId;
    private Double moneyIn;
    private Double moneyOut;

    public Double getMoneyIn() {
        return round(this.moneyIn);
    }

    public Double getMoneyOut() {
        return round(this.moneyOut);
    }

    public Double getNet() {
        return round(this.moneyIn + this.moneyOut);
    }

    /**
     * What share of the month's income was left over. Null rather than zero when nothing came in, so the UI
     * can leave the month blank instead of drawing a 0% that would read as "kept nothing" when the truth is
     * "there is nothing to divide by".
     */
    public Double getSavedPercent() {
        if (this.moneyIn == null || this.moneyIn == 0.0) {
            return null;
        }
        return round(((this.moneyIn + this.moneyOut) / this.moneyIn) * 100.0);
    }

    private static Double round(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
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

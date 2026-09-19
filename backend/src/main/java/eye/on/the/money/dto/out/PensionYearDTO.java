package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class PensionYearDTO implements CSVHelper {

    private String scenario;

    private Integer year;

    private Integer age;

    private boolean working;

    private boolean counted;

    private BigDecimal grossMonthly;

    private BigDecimal netMonthly;

    private BigDecimal valorizedNetMonthly;

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Scenario", "Year", "Age", "Working", "Counted in the pension base",
                "Gross monthly (today's money)", "Pension net monthly (today's money)",
                "Valorized net monthly (today's money)"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getScenario(), this.getYear(), this.getAge(), this.isWorking(),
                this.isCounted(), this.getGrossMonthly(), this.getNetMonthly(), this.getValorizedNetMonthly()};
    }
}

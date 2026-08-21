package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * One year of the projection, on a timeline that runs from today through to the end of the plan.
 */
@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class FireYearDTO implements CSVHelper {

    /** Years from today. Year 0 is the starting position. */
    private Integer year;

    private Integer age;

    /** ACCUMULATION while still paying in, DRAWDOWN once withdrawals have started. */
    private String phase;

    private BigDecimal contributions;

    /** Pension paid during the year, which is income the pot does not have to provide. */
    private BigDecimal pension;

    /** Drawn from the pot itself, after any pension has covered its share of the spending. */
    private BigDecimal withdrawals;

    /** The pot at the end of the year, in the money of that year. */
    private BigDecimal balance;

    /** The same pot expressed in today's money, so it can be compared against the target. */
    private BigDecimal realBalance;

    /** How far {@link #realBalance} has got towards the FIRE number. */
    private BigDecimal pctOfFireNumber;

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Year", "Age", "Phase", "Contributions", "Pension", "Withdrawals", "Balance",
                "Balance (today's money)", "% of FIRE number"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getYear(), this.getAge(), this.getPhase(), this.getContributions(),
                this.getPension(), this.getWithdrawals(), this.getBalance(), this.getRealBalance(),
                this.getPctOfFireNumber()};
    }
}

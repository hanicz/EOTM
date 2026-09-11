package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class FireSnapshotDTO {

    private String currency;

    private BigDecimal netWorth;

    private BigDecimal fireNumber;

    private String targetSource;

    private BigDecimal progressPct;

    private Integer yearsToFire;

    private boolean fiReached;

    private BigDecimal monthlyIncome;

    private BigDecimal monthlySpending;

    private BigDecimal monthlySavings;

    private BigDecimal savingsRatePct;

    private BigDecimal withdrawalRate;
    private BigDecimal annualReturn;
    private BigDecimal annualContributionIncrease;
    private BigDecimal inflation;

    private int horizonYears;

    private boolean hasCashFlow;

    private int monthsCounted;

    private String windowStart;
    private String windowEnd;

    private List<String> ignoredCurrencies;

    private List<String> unconvertedCurrencies;
}

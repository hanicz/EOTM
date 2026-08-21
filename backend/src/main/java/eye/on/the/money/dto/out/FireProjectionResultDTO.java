package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * A worked-through retirement plan: what the pot does each year, when it clears the target, and whether it
 * lasts.
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
public class FireProjectionResultDTO {

    private String currency;

    private BigDecimal portfolioValue;
    private BigDecimal otherAssets;
    private BigDecimal startingValue;

    /** Currencies whose holdings could not be converted, passed through from the portfolio valuation. */
    private List<String> unconvertedCurrencies;

    /** Every year from today to the end of the plan, accumulation and drawdown in one series. */
    private List<FireYearDTO> timeline;

    /** The subset of {@link #timeline} at 1, 3, 5, 10, 15 and 20 years, plus the year the target is met. */
    private List<FireYearDTO> milestones;

    private BigDecimal fireNumber;

    /** Whether the target is a today's-money figure, and so measured against the discounted balance. */
    private boolean fireNumberInTodaysMoney;

    /** Spending the target supports, always in today's money; the drawdown inflates it from here. */
    private BigDecimal annualSpending;
    private BigDecimal withdrawalRate;

    /** The cash actually drawn in the first year of retirement, in the money of that year. */
    private BigDecimal firstYearWithdrawal;

    /** True when the FIRE number was supplied directly rather than derived from spending. */
    private boolean fireNumberOverridden;

    /** Whether the target is reached inside the plan, on whichever basis {@link #fireNumberInTodaysMoney} sets. */
    private boolean fiReached;
    private Integer fiYear;
    private Integer fiAge;

    private Integer retirementYear;
    private Integer retirementAge;

    /** Age the pot runs out, or null if it survives to the end of the plan. */
    private Integer depletedAtAge;
    private boolean lastsThroughRetirement;

    private Integer finalAge;
    private BigDecimal finalBalance;
    private BigDecimal finalRealBalance;
}

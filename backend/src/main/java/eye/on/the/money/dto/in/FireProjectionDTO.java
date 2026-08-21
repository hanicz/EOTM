package eye.on.the.money.dto.in;

import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * The assumptions behind a retirement projection. Nothing here is persisted; every field arrives with the
 * request.
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
public class FireProjectionDTO {

    /** Currency to report in. The live portfolio is converted into it. */
    private String currency;

    /** Savings, cash and anything else not tracked as a holding, added to the live portfolio. */
    @PositiveOrZero
    private BigDecimal otherAssets;

    @NotNull
    @PositiveOrZero
    private BigDecimal monthlyContribution;

    /** Yearly rise in the amount contributed, tracking pay rises. Zero keeps it flat. */
    @DecimalMin("-100.0")
    @DecimalMax("100.0")
    private BigDecimal annualContributionIncrease;

    @NotNull
    @DecimalMin("-100.0")
    @DecimalMax("100.0")
    private BigDecimal annualReturn;

    /** Exclusive of -100: prices falling to nothing leaves today's money undefined. */
    @NotNull
    @DecimalMin(value = "-100.0", inclusive = false)
    @DecimalMax("100.0")
    private BigDecimal inflation;

    /** What you want to live on each year, in today's money. */
    @PositiveOrZero
    private BigDecimal annualSpending;

    /** Share of the pot drawn in the first year of retirement. The 4% rule by default. */
    @DecimalMin("0.1")
    @DecimalMax("100.0")
    private BigDecimal withdrawalRate;

    /**
     * Sets the target directly rather than deriving it from spending. When present it wins, and the spending
     * it implies is reported back.
     */
    @PositiveOrZero
    private BigDecimal fireNumber;

    /** Pension income per month once it starts, in today's money. Zero or unset means no pension. */
    @PositiveOrZero
    private BigDecimal monthlyPension;

    /** Age the pension starts being paid. Unset means no pension, whatever the amount says. */
    @Min(0)
    @Max(120)
    private Integer pensionAge;

    @NotNull
    @Min(0)
    @Max(120)
    private Integer currentAge;

    /** Age contributions stop and withdrawals start. Unset means retire as soon as the target is reached. */
    @Min(0)
    @Max(120)
    private Integer retirementAge;

    /** How long the pot has to last. */
    @Min(1)
    @Max(130)
    private Integer lifeExpectancy;
}

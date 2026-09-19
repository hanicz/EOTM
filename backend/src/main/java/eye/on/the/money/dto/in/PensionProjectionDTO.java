package eye.on.the.money.dto.in;

import eye.on.the.money.model.salary.DegressioMode;
import eye.on.the.money.util.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class PensionProjectionDTO {

    @NotNull
    @Min(16)
    @Max(120)
    private Integer currentAge;

    @NotNull
    @Min(16)
    @Max(120)
    private Integer stopWorkingAge;

    @Min(40)
    @Max(75)
    private Integer retirementAge;

    @NotNull
    @Min(0)
    @Max(60)
    private Integer yearsAlreadyWorked;

    @PositiveOrZero
    private BigDecimal priorAverageGrossMonthly;

    @PositiveOrZero
    private BigDecimal currentGrossMonthlyOverride;

    @DecimalMin("-10.0")
    @DecimalMax("20.0")
    private BigDecimal realWageGrowth;

    @DecimalMin(value = "-100.0", inclusive = false)
    @DecimalMax("100.0")
    private BigDecimal inflation;

    private DegressioMode degresszio;

    private Boolean includeThirteenthMonth;

    @NotEmpty
    @Valid
    private List<PensionScenarioInputDTO> scenarios;
}

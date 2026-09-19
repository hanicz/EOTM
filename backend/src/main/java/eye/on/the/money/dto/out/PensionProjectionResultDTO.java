package eye.on.the.money.dto.out;

import eye.on.the.money.model.salary.DegressioMode;
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
public class PensionProjectionResultDTO {

    private String currency;

    private Integer currentAge;

    private Integer stopWorkingAge;

    private Integer retirementAge;

    private Integer retirementYear;

    private Integer gapYears;

    private Integer serviceYears;

    private BigDecimal scalePct;

    private boolean eligible;

    private boolean partialPension;

    private DegressioMode degresszio;

    private BigDecimal degressioLowerThreshold;

    private BigDecimal degressioUpperThreshold;

    private BigDecimal currentGrossMonthly;

    private BigDecimal realWageGrowth;

    private BigDecimal valorizationUplift;

    private List<PensionSensitivityPointDTO> wageGrowthSensitivity;

    private BigDecimal nationalAverageGrossMonthly;

    private BigDecimal nationalAveragePension;

    private BigDecimal nationalAverageMultiple;

    private BigDecimal salaryMultipleOfNationalAverage;

    private List<PensionScenarioResultDTO> scenarios;

    private List<String> warnings;
}

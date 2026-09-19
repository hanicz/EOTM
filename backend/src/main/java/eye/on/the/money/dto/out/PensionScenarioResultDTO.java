package eye.on.the.money.dto.out;

import eye.on.the.money.model.salary.PensionScenarioType;
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
public class PensionScenarioResultDTO {

    private String name;

    private PensionScenarioType type;

    private BigDecimal pensionBaseMonthly;

    private BigDecimal degressedBaseMonthly;

    private BigDecimal monthlyPension;

    private BigDecimal annualPension;

    private BigDecimal finalGrossMonthly;

    private BigDecimal finalNetMonthly;

    private BigDecimal replacementRatePct;

    private boolean minimumApplied;

    private List<PensionStopAgePointDTO> byStopAge;

    private List<PensionYearDTO> timeline;
}

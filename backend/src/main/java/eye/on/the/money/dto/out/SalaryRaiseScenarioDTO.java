package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SalaryRaiseScenarioDTO {

    private BigDecimal percent;
    private BigDecimal grossMonthly;
    private BigDecimal grossAnnual;
    private BigDecimal netMonthly;
    private BigDecimal netAnnual;
}

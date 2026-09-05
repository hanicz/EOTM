package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SalaryNetDTO {

    private BigDecimal szjaMonthly;
    private BigDecimal tbMonthly;
    private BigDecimal netMonthly;
    private BigDecimal familyAllowanceMonthly;
    private boolean familyAllowanceApplied;
}

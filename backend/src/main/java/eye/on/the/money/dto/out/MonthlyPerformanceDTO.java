package eye.on.the.money.dto.out;

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
public class MonthlyPerformanceDTO {

    private String month;

    private BigDecimal endWorth;
    private BigDecimal change;
    private BigDecimal changePct;
    private BigDecimal contributions;
    private BigDecimal marketGain;
}

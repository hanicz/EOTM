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
public class PensionStopAgePointDTO {

    private Integer stopAge;

    private Integer serviceYears;

    private BigDecimal scalePct;

    private BigDecimal monthlyPension;
}

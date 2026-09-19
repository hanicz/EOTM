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
public class PensionSensitivityPointDTO {

    private BigDecimal realWageGrowth;

    private BigDecimal valorizationUplift;

    private BigDecimal monthlyPension;

    private boolean selected;
}

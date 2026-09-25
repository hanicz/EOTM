package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class PensionDTO {

    @NotNull
    @PositiveOrZero
    private BigDecimal totalContribution;

    @NotNull
    @PositiveOrZero
    private BigDecimal currentValue;

    private String currency;
}

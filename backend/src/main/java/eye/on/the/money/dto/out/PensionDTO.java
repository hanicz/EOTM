package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

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
    private Double totalContribution;

    @NotNull
    @PositiveOrZero
    private Double currentValue;

    private String currency;
}

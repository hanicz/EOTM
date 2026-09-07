package eye.on.the.money.dto.in;

import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * A plain HUF amount to run through the tax method - no rate or price lookups involved.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class TaxAmountDTO {

    @NotNull
    private BigDecimal amount;
}

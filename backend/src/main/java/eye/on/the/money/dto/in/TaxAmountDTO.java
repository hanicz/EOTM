package eye.on.the.money.dto.in;

import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * A plain HUF amount to run through the tax method - no rate or price lookups involved.
 */
@Slf4j
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

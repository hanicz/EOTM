package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * What one asset class cost and what it is worth now, both already converted to the requested currency.
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
public class AssetClassValueDTO {

    private String assetClass;
    private BigDecimal spent;
    private BigDecimal worth;
    private BigDecimal changePct;
    private BigDecimal expectedRatePct;
}

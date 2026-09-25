package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@Generated
public class ForexWatchDTO {
    private Long forexWatchID;
    private BigDecimal liveValue;
    private Boolean stalePrice;
    private String fromCurrencyId;
    private String toCurrencyId;
    private BigDecimal change;
    private Double pChange;
}

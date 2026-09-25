package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Generated
public class StockWatchDTO {
    private Long tickerWatchId;
    private BigDecimal liveValue;
    private Boolean stalePrice;
    private String stockShortName;
    private String stockExchange;
    private String stockName;
    private String currencyId;
    private BigDecimal change;
    private Double pChange;
    private Long groupId;
}

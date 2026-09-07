package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

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
    private Double liveValue;
    private Boolean stalePrice;
    private String stockShortName;
    private String stockExchange;
    private String stockName;
    private String currencyId;
    private Double change;
    private Double pChange;
    private Long groupId;
    private String groupName;
}

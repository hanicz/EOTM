package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Generated
public class StockWatchDTO {
    private Long tickerWatchId;
    private Double liveValue;
    private String stockShortName;
    private String stockExchange;
    private String stockName;
    private String currencyId;
    private Double change;
    private Double pChange;
}

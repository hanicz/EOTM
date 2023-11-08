package eye.on.the.money.dto.out;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

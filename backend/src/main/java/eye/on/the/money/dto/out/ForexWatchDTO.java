package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ForexWatchDTO {
    private Long forexWatchID;
    private Double liveValue;
    private String fromCurrencyId;
    private String toCurrencyId;
    private Double change;
    private Double pChange;
}

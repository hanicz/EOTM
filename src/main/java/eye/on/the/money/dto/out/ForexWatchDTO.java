package eye.on.the.money.dto.out;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ForexWatchDTO {
    private Long forexWatchID;
    private Double liveValue;
    private String fromCurrencyId;
    private String toCurrencyId;
}

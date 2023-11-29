package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CryptoWatchDTO {
    private Long cryptoWatchId;
    private String coinId;
    private String symbol;
    private Double liveValue;
    private String name;
    private Double change;

}

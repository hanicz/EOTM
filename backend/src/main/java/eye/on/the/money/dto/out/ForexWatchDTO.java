package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

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
    private Double liveValue;
    private Boolean stalePrice;
    private String fromCurrencyId;
    private String toCurrencyId;
    private Double change;
    private Double pChange;
}

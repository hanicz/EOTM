package eye.on.the.money.model.stock;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Slf4j
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Recommendation {
    private Integer buy;
    private Integer hold;
    private Date period;
    private Integer sell;
    private Integer strongBuy;
    private Integer strongSell;
    private String symbol;
}

package eye.on.the.money.model.stock;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {
    private Integer buy;
    private Integer hold;
    private Date period;
    private Integer sell;
    private Integer strongBuy;
    private Integer strongSell;
    private String symbol;
}

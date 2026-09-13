package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class NetWorthPointDTO {

    private LocalDate date;

    private BigDecimal totalSpent;
    private BigDecimal totalWorth;

    private Map<String, BigDecimal> assetWorth;
}

package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class StockAlertDTO {

    private Long id;
    private String type;
    private Double valuePoint;
    private String shortName;
    private String exchange;
    private String name;
}

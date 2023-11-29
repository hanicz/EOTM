package eye.on.the.money.dto.in;

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
public class TaxEntry {
    private Long investmentId;
    private Integer quantityUsed;
}

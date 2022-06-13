package eye.on.the.money.dto.in;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaxEntry {
    private Long investmentId;
    private Integer quantityUsed;
}

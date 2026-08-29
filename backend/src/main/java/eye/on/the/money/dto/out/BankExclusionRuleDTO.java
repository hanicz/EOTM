package eye.on.the.money.dto.out;

import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class BankExclusionRuleDTO {

    private Long id;
    private String accountNumber;
    private AccountSide side;
    private boolean active;
}

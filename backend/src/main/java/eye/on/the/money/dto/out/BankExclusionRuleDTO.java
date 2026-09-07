package eye.on.the.money.dto.out;

import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.util.Generated;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class BankExclusionRuleDTO {

    private Long id;
    private String name;
    private String accountNumber;
    private AccountSide side;
    private boolean active;
}

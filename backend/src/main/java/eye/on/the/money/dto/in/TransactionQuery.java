package eye.on.the.money.dto.in;

import eye.on.the.money.util.Generated;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class TransactionQuery {
    private String currency;
    private String type;
}

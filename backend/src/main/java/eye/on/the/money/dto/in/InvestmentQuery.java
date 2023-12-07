package eye.on.the.money.dto.in;


import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Slf4j
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class InvestmentQuery {
    private String currency;
    private String type;
    private Date transactionDateStart;
    private Date transactionDateEnd;
}

package eye.on.the.money.dto.in;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class InvestmentQuery {
    private String currency;
    private String type;
    private Date transactionDateStart;
    private Date transactionDateEnd;
}

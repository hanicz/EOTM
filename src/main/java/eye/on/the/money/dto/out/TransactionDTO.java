package eye.on.the.money.dto.out;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
    private Long transactionId;
    private Double quantity;
    private String buySell;
    private String transactionString;
    private Date transactionDate;
    private String name;
    private Double amount;
    private String currencyName;
}

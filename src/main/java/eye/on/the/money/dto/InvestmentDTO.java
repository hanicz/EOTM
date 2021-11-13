package eye.on.the.money.dto;


import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class InvestmentDTO {
    private Long investmentId;
    private Integer quantity;
    private String buySell;
    private Date transactionDate;
    private String shortName;
    private Double amount;
    private String currencyId;
}

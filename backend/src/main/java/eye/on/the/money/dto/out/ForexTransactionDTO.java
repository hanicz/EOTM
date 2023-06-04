package eye.on.the.money.dto.out;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ForexTransactionDTO {

    private Long forexTransactionId;
    private Double fromAmount;
    private Double toAmount;
    private Date transactionDate;
    private String buySell;
    private Double changeRate;
    private Double liveValue;
    private Double liveChangeRate;
    private Double valueDiff;
    private String fromCurrencyId;
    private String toCurrencyId;

    public ForexTransactionDTO mergeTransactions(ForexTransactionDTO other) {
        this.setFromAmount(this.getFromAmount() + other.getFromAmount());
        this.setToAmount(this.getToAmount() + other.getToAmount());
        this.setChangeRate(this.getFromAmount() / this.getToAmount());

        return this;
    }

}
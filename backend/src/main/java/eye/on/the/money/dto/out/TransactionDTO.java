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
public class TransactionDTO {
    private Long transactionId;
    private Double quantity;
    private String buySell;
    private String transactionString;
    private Date transactionDate;
    private String symbol;
    private String coinId;
    private Double amount;
    private String currencyId;
    private Double liveValue;
    private Double valueDiff;
    private Double fee;

    public TransactionDTO mergeTransactions(TransactionDTO other) {
        if (!this.getSymbol().equals(other.getSymbol()))
            return this;

        this.setAmount(this.getAmount() + other.getAmount());
        this.setQuantity(this.getQuantity() + other.getQuantity());

        if (this.getQuantity() > 0 && "S".equals(this.buySell)) {
            this.buySell = "B";
        }
        return this;
    }

    public void negateAmountAndQuantity() {
        this.amount = -this.amount;
        this.quantity = -this.quantity;
    }
}

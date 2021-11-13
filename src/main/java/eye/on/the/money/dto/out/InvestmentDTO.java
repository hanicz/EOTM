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
public class InvestmentDTO {
    private Long investmentId;
    private Integer quantity;
    private String buySell;
    private Date transactionDate;
    private String shortName;
    private Double amount;
    private String currencyId;

    public InvestmentDTO mergeInvestments(InvestmentDTO other){
        if(!this.getShortName().equals(other.getShortName()))
            return this;

        this.setAmount(this.getAmount() + other.getAmount());
        this.setQuantity(this.getQuantity() + other.getQuantity());

        if(this.getQuantity() > 0 && this.buySell.equals("S")){
            this.buySell = "B";
        }
        return this;
    }

    public void negateAmountAndQuantity(){
        this.amount = -this.amount;
        this.quantity = -this.quantity;
    }
}

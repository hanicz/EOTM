package eye.on.the.money.dto.out;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class ForexTransactionDTO {

    private Long forexTransactionId;
    private Double fromAmount;
    private Double toAmount;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
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

package eye.on.the.money.dto.out;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.dto.CSVHelper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class ForexTransactionDTO implements CSVHelper{

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

    @Override
    public Object[] getHeaders() {
        return new String[]{"Transaction Id", "From Amount", "To Amount", "Type", "Transaction Date", "Change Rate", "From Currency", "To Currency"};
    }

    @Override
    public Object[] getCSVRecord() {
        return new Object[]{this.getForexTransactionId(), this.getFromAmount(), this.getToAmount(),
                this.getBuySell(), this.getTransactionDate(), this.getChangeRate(), this.getFromCurrencyId(), this.getToCurrencyId()};
    }

    public static ForexTransactionDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return ForexTransactionDTO.builder()
                .forexTransactionId(csvRecord.get("Transaction Id").isBlank() ? null : Long.parseLong(csvRecord.get("Transaction Id")))
                .buySell(csvRecord.get("Type"))
                .transactionDate(LocalDate.parse(csvRecord.get("Transaction Date"), formatter))
                .fromAmount(Double.parseDouble(csvRecord.get("From Amount")))
                .toAmount(Double.parseDouble(csvRecord.get("To Amount")))
                .toCurrencyId(csvRecord.get("To Currency"))
                .fromCurrencyId(csvRecord.get("From Currency"))
                .changeRate(Double.parseDouble(csvRecord.get("Change Rate")))
                .build();
    }
}

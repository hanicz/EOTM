package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class TransactionDTO implements CSVHelper {
    private Long transactionId;
    private Double quantity;
    private String buySell;
    private String transactionString;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
    private String symbol;
    private String coinId;
    private Double amount;
    private String currencyId;
    private Double liveValue;
    private Double valueDiff;
    private Double fee;
    private String url;

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

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency", "Fee"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getTransactionId(), this.getQuantity(),
                this.getBuySell(), this.getTransactionDate(), this.getSymbol(),
                this.getAmount(), this.getCurrencyId(), this.getFee()};
    }

    public static TransactionDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return TransactionDTO.builder()
                .transactionId(csvRecord.get("Transaction Id").isBlank() ? null : Long.parseLong(csvRecord.get("Transaction Id")))
                .buySell(csvRecord.get("Type"))
                .transactionDate(LocalDate.parse(csvRecord.get("Transaction Date"), formatter))
                .amount(Double.parseDouble(csvRecord.get("Amount")))
                .quantity(Double.parseDouble(csvRecord.get("Quantity")))
                .currencyId(csvRecord.get("Currency"))
                .symbol(csvRecord.get("Symbol"))
                .fee(Double.parseDouble(csvRecord.get("Fee")))
                .build();
    }
}

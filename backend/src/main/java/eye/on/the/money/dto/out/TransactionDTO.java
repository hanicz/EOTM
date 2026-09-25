package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.dto.Lot;
import lombok.*;
import org.apache.commons.csv.CSVRecord;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class TransactionDTO implements CSVHelper, Serializable, Lot<TransactionDTO> {

    private Long id;
    private BigDecimal quantity;
    private String buySell;
    private String transactionString;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
    private String symbol;
    private String coinId;
    private BigDecimal amount;
    private String currencyId;
    private BigDecimal liveValue;
    private BigDecimal valueDiff;
    private BigDecimal fee;
    private String url;

    @Override
    public TransactionDTO merge(TransactionDTO other) {
        if (!this.getSymbol().equals(other.getSymbol()))
            return this;

        this.setAmount(this.getAmount().add(other.getAmount()));
        this.setQuantity(this.getQuantity().add(other.getQuantity()));

        if (this.getQuantity().signum() > 0 && "S".equals(this.buySell)) {
            this.buySell = "B";
        }
        return this;
    }

    @Override
    public void negateAmountAndQuantity() {
        this.amount = this.amount.negate();
        this.quantity = this.quantity.negate();
    }

    @Override
    public Long recordId() {
        return this.id;
    }

    @Override
    @JsonIgnore
    public boolean isClosed() {
        return this.quantity != null && this.quantity.signum() == 0;
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency", "Fee"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getId(), CSVHelper.plainNumber(this.getQuantity()),
                this.getBuySell(), this.getTransactionDate(), this.getSymbol(),
                CSVHelper.plainNumber(this.getAmount()), this.getCurrencyId(), CSVHelper.plainNumber(this.getFee())};
    }

    public static TransactionDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return TransactionDTO.builder()
                .id(csvRecord.get("Transaction Id").isBlank() ? null : Long.parseLong(csvRecord.get("Transaction Id")))
                .buySell(csvRecord.get("Type"))
                .transactionDate(LocalDate.parse(csvRecord.get("Transaction Date"), formatter))
                .amount(new BigDecimal(csvRecord.get("Amount")))
                .quantity(new BigDecimal(csvRecord.get("Quantity")))
                .currencyId(csvRecord.get("Currency"))
                .symbol(csvRecord.get("Symbol"))
                .fee(new BigDecimal(csvRecord.get("Fee")))
                .build();
    }
}

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
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ETFInvestmentDTO implements CSVHelper {

    private Long id;
    private Integer quantity;
    private String buySell;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
    private String shortName;
    private Double amount;
    private String currencyId;
    private Double liveValue;
    private Double valueDiff;
    private Double fee;
    private String exchange;

    public ETFInvestmentDTO mergeInvestments(ETFInvestmentDTO other) {
        if (!this.getShortName().equals(other.getShortName()))
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
    public Object[] getHeaders() {
        return new String[]{"Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Exchange", "Amount", "Currency", "Fee"};
    }

    @Override
    public Object[] getCSVRecord() {
        return new Object[]{this.getId(), this.getQuantity(), this.getBuySell(), this.getTransactionDate(),
                this.getShortName(), this.getExchange(), this.getAmount(), this.getCurrencyId(), this.getFee()};
    }

    public static ETFInvestmentDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return ETFInvestmentDTO.builder()
                .id(csvRecord.get("Investment Id").isBlank() ? null : Long.parseLong(csvRecord.get("Investment Id")))
                .buySell(csvRecord.get("Type"))
                .transactionDate(LocalDate.parse(csvRecord.get("Transaction Date"), formatter))
                .amount(Double.parseDouble(csvRecord.get("Amount")))
                .quantity(Integer.parseInt(csvRecord.get("Quantity")))
                .currencyId(csvRecord.get("Currency"))
                .shortName(csvRecord.get("Short Name"))
                .exchange(csvRecord.get("Exchange"))
                .fee(Double.parseDouble(csvRecord.get("Fee")))
                .build();
    }
}

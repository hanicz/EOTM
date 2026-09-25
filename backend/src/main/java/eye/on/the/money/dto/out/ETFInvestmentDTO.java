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
import java.util.Objects;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ETFInvestmentDTO implements CSVHelper, Serializable, Lot<ETFInvestmentDTO> {

    private Long id;
    private BigDecimal quantity;
    private String buySell;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
    private String shortName;
    private String name;
    private BigDecimal amount;
    private String currencyId;
    private BigDecimal liveValue;
    private Boolean stalePrice;
    private BigDecimal valueDiff;
    private BigDecimal dayChange;
    private Double dayChangePercent;
    private BigDecimal fee;
    private String exchange;
    private String accountName;
    private Long accountId;

    @Override
    public ETFInvestmentDTO merge(ETFInvestmentDTO other) {
        if (!this.getShortName().equals(other.getShortName())
                || !Objects.equals(this.getExchange(), other.getExchange())
                || !Objects.equals(this.getAccountId(), other.getAccountId()))
            return this;

        this.setAmount(this.getAmount().add(other.getAmount()));
        this.setQuantity(this.getQuantity().add(other.getQuantity()));

        if (this.getQuantity().signum() > 0 && "S".equals(this.buySell)) {
            this.buySell = "B";
        }
        return this;
    }

    @Override
    public Long recordId() {
        return this.id;
    }

    @Override
    public void negateAmountAndQuantity() {
        this.amount = this.amount.negate();
        this.quantity = this.quantity.negate();
    }

    @Override
    @JsonIgnore
    public boolean isClosed() {
        return this.quantity != null && this.quantity.signum() == 0;
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Exchange", "Amount", "Currency", "Fee", "Account"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getId(), CSVHelper.plainNumber(this.getQuantity()), this.getBuySell(), this.getTransactionDate(),
                this.getShortName(), this.getExchange(), CSVHelper.plainNumber(this.getAmount()), this.getCurrencyId(),
                CSVHelper.plainNumber(this.getFee()), this.getAccountName()};
    }

    public static ETFInvestmentDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return ETFInvestmentDTO.builder()
                .id(csvRecord.get("Investment Id").isBlank() ? null : Long.parseLong(csvRecord.get("Investment Id")))
                .buySell(csvRecord.get("Type"))
                .transactionDate(LocalDate.parse(csvRecord.get("Transaction Date"), formatter))
                .amount(new BigDecimal(csvRecord.get("Amount")))
                .quantity(new BigDecimal(csvRecord.get("Quantity")))
                .currencyId(csvRecord.get("Currency"))
                .shortName(csvRecord.get("Short Name"))
                .exchange(csvRecord.get("Exchange"))
                .fee(new BigDecimal(csvRecord.get("Fee")))
                .accountName(csvRecord.get("Account"))
                .build();
    }
}

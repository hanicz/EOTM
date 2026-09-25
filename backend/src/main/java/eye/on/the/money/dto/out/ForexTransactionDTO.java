package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.dto.Lot;
import eye.on.the.money.util.Numbers;
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
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class ForexTransactionDTO implements CSVHelper, Serializable, Lot<ForexTransactionDTO> {

    private Long forexTransactionId;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
    private String buySell;
    private BigDecimal changeRate;
    private BigDecimal liveValue;
    private BigDecimal liveChangeRate;
    private Boolean stalePrice;
    private BigDecimal valueDiff;
    private String fromCurrencyId;
    private String toCurrencyId;

    @Override
    public void negateAmountAndQuantity() {
        BigDecimal heldAmount = this.fromAmount;
        String heldCurrencyId = this.fromCurrencyId;

        this.fromAmount = this.toAmount.negate();
        this.fromCurrencyId = this.toCurrencyId;
        this.toAmount = heldAmount.negate();
        this.toCurrencyId = heldCurrencyId;
    }

    @Override
    public ForexTransactionDTO merge(ForexTransactionDTO other) {
        if (!Objects.equals(this.getFromCurrencyId(), other.getFromCurrencyId())
                || !Objects.equals(this.getToCurrencyId(), other.getToCurrencyId()))
            return this;

        this.setFromAmount(this.getFromAmount().add(other.getFromAmount()));
        this.setToAmount(this.getToAmount().add(other.getToAmount()));

        if (this.getToAmount().signum() != 0) {
            this.setChangeRate(Numbers.divide(this.getFromAmount(), this.getToAmount()));
        }

        if (this.getToAmount().signum() > 0 && "S".equals(this.buySell)) {
            this.buySell = "B";
        }
        return this;
    }

    @Override
    public Long recordId() {
        return this.forexTransactionId;
    }

    @Override
    @JsonIgnore
    public boolean isClosed() {
        return this.toAmount != null && this.toAmount.signum() == 0;
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Transaction Id", "From Amount", "To Amount", "Type", "Transaction Date", "Change Rate", "From Currency", "To Currency"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getForexTransactionId(), CSVHelper.plainNumber(this.getFromAmount()),
                CSVHelper.plainNumber(this.getToAmount()), this.getBuySell(), this.getTransactionDate(),
                CSVHelper.plainNumber(this.getChangeRate()), this.getFromCurrencyId(), this.getToCurrencyId()};
    }

    public static ForexTransactionDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return ForexTransactionDTO.builder()
                .forexTransactionId(csvRecord.get("Transaction Id").isBlank() ? null : Long.parseLong(csvRecord.get("Transaction Id")))
                .buySell(csvRecord.get("Type"))
                .transactionDate(LocalDate.parse(csvRecord.get("Transaction Date"), formatter))
                .fromAmount(new BigDecimal(csvRecord.get("From Amount")))
                .toAmount(new BigDecimal(csvRecord.get("To Amount")))
                .toCurrencyId(csvRecord.get("To Currency"))
                .fromCurrencyId(csvRecord.get("From Currency"))
                .changeRate(new BigDecimal(csvRecord.get("Change Rate")))
                .build();
    }
}

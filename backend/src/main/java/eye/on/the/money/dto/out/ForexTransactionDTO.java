package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.dto.Lot;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class ForexTransactionDTO implements CSVHelper, Serializable, Lot<ForexTransactionDTO> {

    private static final double CLOSED_TOLERANCE = 1e-6;

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
    private Boolean stalePrice;
    private Double valueDiff;
    private String fromCurrencyId;
    private String toCurrencyId;

    @Override
    public void negateAmountAndQuantity() {
        Double heldAmount = this.fromAmount;
        String heldCurrencyId = this.fromCurrencyId;

        this.fromAmount = -this.toAmount;
        this.fromCurrencyId = this.toCurrencyId;
        this.toAmount = -heldAmount;
        this.toCurrencyId = heldCurrencyId;
    }

    @Override
    public ForexTransactionDTO merge(ForexTransactionDTO other) {
        if (!Objects.equals(this.getFromCurrencyId(), other.getFromCurrencyId())
                || !Objects.equals(this.getToCurrencyId(), other.getToCurrencyId()))
            return this;

        this.setFromAmount(this.getFromAmount() + other.getFromAmount());
        this.setToAmount(this.getToAmount() + other.getToAmount());

        if (Math.abs(this.toAmount) < ForexTransactionDTO.CLOSED_TOLERANCE) {
            this.toAmount = 0.0;
        }

        if (this.getToAmount() != 0.0) {
            this.setChangeRate(this.getFromAmount() / this.getToAmount());
        }

        if (this.getToAmount() > 0 && "S".equals(this.buySell)) {
            this.buySell = "B";
        }
        return this;
    }

    @Override
    @JsonIgnore
    public boolean isClosed() {
        return this.toAmount != null && this.toAmount == 0.0;
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Transaction Id", "From Amount", "To Amount", "Type", "Transaction Date", "Change Rate", "From Currency", "To Currency"};
    }

    @Override
    @JsonIgnore
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

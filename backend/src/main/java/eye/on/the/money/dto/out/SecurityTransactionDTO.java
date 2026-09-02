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
public class SecurityTransactionDTO implements CSVHelper, Lot<SecurityTransactionDTO> {
    private Long transactionId;
    private Integer quantity;
    private String buySell;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
    private String securityId;
    private String securityName;
    private Double amount;
    private String currencyId;
    private Double rate;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate nextPaymentDate;
    private Double nextPaymentAmount;
    private Boolean zeroCoupon;

    @Override
    public SecurityTransactionDTO merge(SecurityTransactionDTO other) {
        if (!this.getSecurityId().equals(other.getSecurityId()))
            return this;

        this.setAmount(this.getAmount() + other.getAmount());
        this.setQuantity(this.getQuantity() + other.getQuantity());

        if (this.getQuantity() > 0 && "S".equals(this.buySell)) {
            this.buySell = "B";
        }
        return this;
    }

    @Override
    public void negateAmountAndQuantity() {
        this.amount = -this.amount;
        this.quantity = -this.quantity;
    }

    @Override
    @JsonIgnore
    public boolean isClosed() {
        return this.quantity != null && this.quantity == 0;
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Transaction Id", "Quantity", "Type", "Transaction Date", "Security Id", "Security Name",
                "Amount", "Currency"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getTransactionId(), this.getQuantity(),
                this.getBuySell(), this.getTransactionDate(), this.getSecurityId(), this.getSecurityName(),
                this.getAmount(), this.getCurrencyId()};
    }

    public static SecurityTransactionDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return SecurityTransactionDTO.builder()
                .transactionId(csvRecord.get("Transaction Id").isBlank() ? null : Long.parseLong(csvRecord.get("Transaction Id")))
                .buySell(csvRecord.get("Type"))
                .transactionDate(LocalDate.parse(csvRecord.get("Transaction Date"), formatter))
                .amount(Double.parseDouble(csvRecord.get("Amount")))
                .quantity(Integer.parseInt(csvRecord.get("Quantity")))
                .currencyId(csvRecord.get("Currency"))
                .securityId(csvRecord.get("Security Id"))
                .securityName(csvRecord.get("Security Name"))
                .build();
    }
}

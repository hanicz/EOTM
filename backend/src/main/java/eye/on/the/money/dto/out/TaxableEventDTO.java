package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.dto.CSVHelper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class TaxableEventDTO implements CSVHelper {

    private Long id;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate bookingDate;
    private String type;
    private String partnerName;
    private String memo;
    private BigDecimal amount;
    private String currencyId;
    private BigDecimal rate;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate rateDate;
    private BigDecimal amountInHuf;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate calculatedOn;
    private TaxBreakdownDTO tax;
    private boolean paid;

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Id", "Booking Date", "Type", "Partner Name", "Memo", "Amount", "Currency",
                "MNB Rate", "Rate Date", "Amount (HUF)", "Tax Base", "Szocho", "Szja", "Tax", "Calculated On", "Paid"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getId(), this.getBookingDate(), this.getType(), this.getPartnerName(),
                this.getMemo(), this.getAmount(), this.getCurrencyId(), this.getRate(), this.getRateDate(),
                this.getAmountInHuf(), this.getTax().getTaxBase(), this.getTax().getSzocho(),
                this.getTax().getSzja(), this.getTax().getTotal(), this.getCalculatedOn(), this.isPaid()};
    }
}

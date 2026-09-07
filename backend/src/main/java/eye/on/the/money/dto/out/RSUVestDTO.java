package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RSUVestDTO implements CSVHelper, Serializable {

    private Long grantId;
    private String shortName;
    private String exchange;
    private LocalDate grantDate;
    private int sequence;
    private LocalDate vestDate;
    private Integer quantity;
    private String currency;
    private BigDecimal price;
    private LocalDate priceDate;
    private BigDecimal amount;
    private BigDecimal rate;
    private LocalDate rateDate;
    private BigDecimal amountInHuf;
    private BigDecimal netInHuf;
    private boolean vested;
    private boolean projected;

    private TaxBreakdownDTO tax;

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Ticker", "Exchange", "Grant Date", "Vest", "Vest Date", "Quantity", "Currency",
                "Price", "Price Date", "Value", "MNB Rate", "Rate Date", "Value (HUF)", "Tax Base", "Szocho",
                "Szja", "Tax", "Net (HUF)", "Projected"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        TaxBreakdownDTO breakdown = (this.getTax() == null) ? TaxBreakdownDTO.zero() : this.getTax();
        return new Object[]{this.getShortName(), this.getExchange(), this.getGrantDate(), this.getSequence(),
                this.getVestDate(), this.getQuantity(), this.getCurrency(), this.getPrice(), this.getPriceDate(),
                this.getAmount(), this.getRate(), this.getRateDate(), this.getAmountInHuf(), breakdown.getTaxBase(),
                breakdown.getSzocho(), breakdown.getSzja(), breakdown.getTotal(), this.getNetInHuf(),
                this.isProjected()};
    }
}

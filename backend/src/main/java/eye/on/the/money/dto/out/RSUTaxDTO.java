package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RSUTaxDTO implements CSVHelper {

    private String shortName;
    private String exchange;
    private LocalDate date;
    private Integer quantity;
    private String currency;

    private BigDecimal price;
    /** The trading day the close came from - earlier than date if that was not a trading day. */
    private LocalDate priceDate;

    private BigDecimal amount;
    private BigDecimal rate;
    private LocalDate rateDate;
    private BigDecimal amountInHuf;

    private TaxBreakdownDTO tax;

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Ticker", "Exchange", "Date", "Quantity", "Currency", "Price", "Price Date",
                "Value", "MNB Rate", "Rate Date", "Value (HUF)", "Tax Base", "Szocho", "Szja", "Tax"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        TaxBreakdownDTO breakdown = (this.getTax() == null) ? TaxBreakdownDTO.zero() : this.getTax();
        return new Object[]{this.getShortName(), this.getExchange(), this.getDate(), this.getQuantity(),
                this.getCurrency(), this.getPrice(), this.getPriceDate(), this.getAmount(), this.getRate(),
                this.getRateDate(), this.getAmountInHuf(), breakdown.getTaxBase(), breakdown.getSzocho(),
                breakdown.getSzja(), breakdown.getTotal()};
    }
}

package eye.on.the.money.dto.out;

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
public class RSUTaxDTO {

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
}

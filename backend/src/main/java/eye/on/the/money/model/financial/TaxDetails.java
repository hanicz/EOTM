package eye.on.the.money.model.financial;

import eye.on.the.money.util.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The tax worked out when the transaction was flagged as a taxable event. Stored rather than recomputed,
 * so the report always shows the rate and the amounts that were in force at the time of the flagging.
 */
@Embeddable
@Getter
@Setter
@Builder
@Slf4j
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class TaxDetails {

    @Column(name = "tax_rate")
    private BigDecimal rate;

    @Column(name = "tax_rate_date")
    private LocalDate rateDate;

    @Column(name = "tax_amount_huf")
    private BigDecimal amountInHuf;

    @Column(name = "tax_base")
    private BigDecimal taxBase;

    @Column(name = "tax_szocho")
    private BigDecimal szocho;

    @Column(name = "tax_szja")
    private BigDecimal szja;

    @Column(name = "tax_total")
    private BigDecimal total;

    @Column(name = "tax_calculated_on")
    private LocalDate calculatedOn;

    @Column(name = "tax_paid")
    private boolean paid;
}

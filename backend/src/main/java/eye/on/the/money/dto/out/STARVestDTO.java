package eye.on.the.money.dto.out;

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
public class STARVestDTO implements Serializable {

    private Long grantId;
    private String name;
    private LocalDate commencementDate;
    private int sequence;
    private LocalDate vestDate;
    private Integer quantity;
    private boolean cliff;
    private String currency;
    private BigDecimal spreadPerUnit;
    private BigDecimal amount;
    private BigDecimal rate;
    private LocalDate rateDate;
    private BigDecimal amountInHuf;
    private BigDecimal netInHuf;
    private boolean vested;
    private boolean projected;

    private TaxBreakdownDTO tax;
}

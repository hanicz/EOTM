package eye.on.the.money.dto.out;

import eye.on.the.money.model.stock.VestingFrequency;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RSUGrantDTO implements Serializable {

    private Long id;
    private String shortName;
    private String exchange;
    private String currency;
    private LocalDate grantDate;
    private Integer quantity;
    private int vestingYears;
    private VestingFrequency vestingFrequency;
    private String note;

    private List<RSUVestDTO> vests;

    private BigDecimal totalAmountInHuf;
    private TaxBreakdownDTO totalTax;
    private BigDecimal totalNetInHuf;
    private BigDecimal vestedNetInHuf;
    private BigDecimal upcomingNetInHuf;

    private String error;
}

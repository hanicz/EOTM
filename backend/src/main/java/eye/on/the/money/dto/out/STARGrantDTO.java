package eye.on.the.money.dto.out;

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
public class STARGrantDTO implements Serializable {

    private Long id;
    private String name;
    private String currency;
    private LocalDate commencementDate;
    private Integer quantity;
    private BigDecimal baseValue;
    private BigDecimal currentValue;
    private BigDecimal spreadPerUnit;
    private int vestingYears;
    private String note;

    private List<STARVestDTO> vests;

    private BigDecimal totalAmountInHuf;
    private TaxBreakdownDTO totalTax;
    private BigDecimal totalNetInHuf;
    private BigDecimal vestedNetInHuf;
    private BigDecimal upcomingNetInHuf;

    private String error;
}

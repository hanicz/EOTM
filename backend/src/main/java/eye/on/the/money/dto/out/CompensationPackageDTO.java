package eye.on.the.money.dto.out;

import eye.on.the.money.model.salary.SalaryBasis;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CompensationPackageDTO {

    private String label;
    private String currencyId;
    private SalaryBasis basis;
    private int dependents;
    private LocalDate validFrom;
    private LocalDate validTo;

    private BigDecimal baseGrossMonthly;
    private BigDecimal baseGrossAnnual;
    private BigDecimal baseNetMonthly;
    private BigDecimal baseNetAnnual;

    private List<CompensationItemDTO> items;

    private BigDecimal totalGrossMonthly;
    private BigDecimal totalGrossAnnual;
    private BigDecimal totalNetMonthly;
    private BigDecimal totalNetAnnual;
}

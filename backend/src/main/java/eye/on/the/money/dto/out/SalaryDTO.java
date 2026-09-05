package eye.on.the.money.dto.out;

import eye.on.the.money.model.salary.SalaryBasis;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SalaryDTO {

    private Long id;
    private BigDecimal amount;
    private SalaryBasis basis;
    private String currencyId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private int dependents;
    private String note;

    private BigDecimal grossMonthly;
    private BigDecimal grossAnnual;
    private BigDecimal netMonthly;
    private BigDecimal netAnnual;
    private BigDecimal szjaMonthly;
    private BigDecimal szjaAnnual;
    private BigDecimal tbMonthly;
    private BigDecimal tbAnnual;
    private BigDecimal familyAllowanceMonthly;
    private boolean familyAllowanceApplied;
}

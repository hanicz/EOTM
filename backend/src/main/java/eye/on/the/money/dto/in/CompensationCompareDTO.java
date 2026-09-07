package eye.on.the.money.dto.in;

import eye.on.the.money.model.salary.CompensationItem;
import eye.on.the.money.model.salary.Salary;
import eye.on.the.money.model.salary.SalaryBasis;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CompensationCompareDTO(@Size(max = CompensationItem.NAME_MAX_LENGTH) String label,
                                     @NotNull @Positive BigDecimal baseAmount,
                                     @NotNull SalaryBasis baseBasis,
                                     @NotBlank String currencyId,
                                     @NotNull @Min(0) @Max(Salary.MAX_DEPENDENTS) Integer dependents,
                                     @Valid List<CompensationItemInputDTO> items) {
}

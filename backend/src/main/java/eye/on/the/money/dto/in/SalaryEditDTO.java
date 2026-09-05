package eye.on.the.money.dto.in;

import eye.on.the.money.model.salary.Salary;
import eye.on.the.money.model.salary.SalaryBasis;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryEditDTO(@NotNull @Positive BigDecimal amount,
                            @NotNull SalaryBasis basis,
                            @NotBlank String currencyId,
                            @NotNull LocalDate validFrom,
                            LocalDate validTo,
                            @NotNull @Min(0) @Max(Salary.MAX_DEPENDENTS) Integer dependents,
                            @Size(max = Salary.NOTE_MAX_LENGTH) String note) {
}

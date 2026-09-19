package eye.on.the.money.dto.in;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PensionStepDTO(@NotNull @Min(16) @Max(120) Integer untilAge,
                             @NotNull @DecimalMin("-50.0") @DecimalMax("50.0") BigDecimal realGrowthPct) {
}

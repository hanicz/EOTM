package eye.on.the.money.dto.in;

import eye.on.the.money.model.salary.PensionScenarioType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PensionScenarioInputDTO(@NotBlank @Size(max = 32) String name,
                                      @NotNull PensionScenarioType type,
                                      @DecimalMin("-50.0") @DecimalMax("50.0") BigDecimal realGrowthPct,
                                      @Valid List<PensionStepDTO> steps) {
}

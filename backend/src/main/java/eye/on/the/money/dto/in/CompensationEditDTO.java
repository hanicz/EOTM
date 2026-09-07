package eye.on.the.money.dto.in;

import eye.on.the.money.model.salary.CompensationAmountMode;
import eye.on.the.money.model.salary.CompensationItem;
import eye.on.the.money.model.salary.CompensationTaxTreatment;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CompensationEditDTO(@NotBlank @Size(max = CompensationItem.NAME_MAX_LENGTH) String name,
                                  @NotNull CompensationAmountMode amountMode,
                                  @Positive BigDecimal monthlyAmount,
                                  @Positive @DecimalMax(CompensationItem.MAX_PERCENT) BigDecimal percent,
                                  @NotNull CompensationTaxTreatment taxTreatment,
                                  @Size(max = CompensationItem.NOTE_MAX_LENGTH) String note) {
}

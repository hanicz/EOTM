package eye.on.the.money.dto.in;

import eye.on.the.money.model.financial.CategoryColor;
import eye.on.the.money.model.financial.SpendingCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpendingCategoryEditDTO(@NotBlank @Size(max = SpendingCategory.NAME_MAX_LENGTH) String name,
                                      @NotNull CategoryColor color,
                                      Integer position) {
}

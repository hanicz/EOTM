package eye.on.the.money.dto.in;

import eye.on.the.money.model.financial.BankCategoryRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BankCategoryRuleEditDTO(@Size(max = BankCategoryRule.NAME_MAX_LENGTH) String name,
                                      @NotBlank @Size(max = BankCategoryRule.PATTERN_MAX_LENGTH) String pattern,
                                      @NotNull Long categoryId,
                                      Integer priority,
                                      @NotNull Boolean active) {
}

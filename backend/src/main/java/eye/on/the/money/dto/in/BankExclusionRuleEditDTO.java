package eye.on.the.money.dto.in;

import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.model.financial.BankExclusionRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BankExclusionRuleEditDTO(@NotBlank @Size(max = BankExclusionRule.ACCOUNT_MAX_LENGTH) String accountNumber,
                                       @NotNull AccountSide side,
                                       @NotNull Boolean active) {
}

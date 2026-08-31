package eye.on.the.money.dto.in;

import eye.on.the.money.model.stock.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AccountEditDTO(@NotBlank @Size(max = Account.NAME_MAX_LENGTH) String accountName,
                             @NotNull LocalDate creationDate) {
}

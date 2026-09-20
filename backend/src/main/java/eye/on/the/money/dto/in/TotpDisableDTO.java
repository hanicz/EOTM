package eye.on.the.money.dto.in;

import eye.on.the.money.model.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TotpDisableDTO(@NotNull @Size(max = User.PASSWORD_MAX_LENGTH) String password) {
}

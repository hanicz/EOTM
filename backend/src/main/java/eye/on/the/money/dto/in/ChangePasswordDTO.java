package eye.on.the.money.dto.in;

import jakarta.validation.constraints.NotNull;

public record ChangePasswordDTO(@NotNull String password) {
}

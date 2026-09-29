package eye.on.the.money.dto.in;

import eye.on.the.money.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TotpChangeDTO(
        @NotNull @Size(max = User.PASSWORD_MAX_LENGTH) String password,
        @NotBlank @Size(max = 16) String code) {
}

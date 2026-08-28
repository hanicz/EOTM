package eye.on.the.money.dto.in;

import eye.on.the.money.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordDTO(
        @NotBlank @Size(min = User.PASSWORD_MIN_LENGTH, max = User.PASSWORD_MAX_LENGTH) String newPassword,
        @NotNull String oldPassword) {
}

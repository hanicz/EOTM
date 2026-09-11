package eye.on.the.money.dto.in;

import eye.on.the.money.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreferencesUpdateDTO(
        @NotBlank @Size(min = User.CURRENCY_LENGTH, max = User.CURRENCY_LENGTH) String preferredCurrency) {
}

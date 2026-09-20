package eye.on.the.money.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TotpCodeDTO(@NotBlank @Size(max = 16) String code) {
}

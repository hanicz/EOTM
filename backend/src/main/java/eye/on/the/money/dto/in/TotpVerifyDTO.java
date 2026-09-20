package eye.on.the.money.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TotpVerifyDTO(
        @NotBlank @Size(max = 4096) String mfaToken,
        @NotBlank @Size(max = 16) String code) {
}

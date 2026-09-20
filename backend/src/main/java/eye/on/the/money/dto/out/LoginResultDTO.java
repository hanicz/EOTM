package eye.on.the.money.dto.out;

public record LoginResultDTO(boolean mfaRequired, String mfaToken) {
}

package eye.on.the.money.dto.out;

public record TotpSetupDTO(String secret, String otpauthUri, String qrPng) {
}

package eye.on.the.money.exception.dto;

public record ErrorResponse(
        int code,
        String error
) {
}

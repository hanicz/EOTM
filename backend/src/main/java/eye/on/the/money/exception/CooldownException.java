package eye.on.the.money.exception;

import lombok.Getter;

import java.time.Duration;

@Getter
public class CooldownException extends RuntimeException {

    private final Duration retryAfter;

    public CooldownException(String errorMsg, Duration retryAfter) {
        super(errorMsg);
        this.retryAfter = retryAfter;
    }
}

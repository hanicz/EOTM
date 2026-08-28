package eye.on.the.money.security;

import org.springframework.security.core.AuthenticationException;

public class MalformedLoginRequestException extends AuthenticationException {

    public MalformedLoginRequestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

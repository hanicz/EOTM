package eye.on.the.money.exception;

public class PasswordException extends RuntimeException {

    public PasswordException(String errorMsg) {
        super(errorMsg);
    }

    public PasswordException(String errorMsg, Throwable t) {
        super(errorMsg, t);
    }
}


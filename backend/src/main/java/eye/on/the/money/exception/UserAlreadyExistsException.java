package eye.on.the.money.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String errorMsg) {
        super(errorMsg);
    }

    public UserAlreadyExistsException(String errorMsg, Throwable t) {
        super(errorMsg, t);
    }
}

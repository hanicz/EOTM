package eye.on.the.money.exception;

public class ValidationException extends IllegalArgumentException {

    public ValidationException(String message) {
        super(message);
    }
}

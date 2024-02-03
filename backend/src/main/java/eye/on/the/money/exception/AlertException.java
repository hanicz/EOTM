package eye.on.the.money.exception;

public class AlertException extends RuntimeException {

    public AlertException(String errorMsg) {
        super(errorMsg);
    }

    public AlertException(String errorMsg, Throwable exc) {
        super(errorMsg, exc);
    }
}

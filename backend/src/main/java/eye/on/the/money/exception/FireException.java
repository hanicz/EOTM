package eye.on.the.money.exception;

public class FireException extends RuntimeException {

    public FireException(String errorMsg) {
        super(errorMsg);
    }

    public FireException(String errorMsg, Throwable exc) {
        super(errorMsg, exc);
    }
}

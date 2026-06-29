package eye.on.the.money.exception;

public class EmailException extends RuntimeException {

    public EmailException(String errorMsg) {
        super(errorMsg);
    }

    public EmailException(String errorMsg, Throwable exc) {
        super(errorMsg, exc);
    }
}

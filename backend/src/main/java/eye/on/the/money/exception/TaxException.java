package eye.on.the.money.exception;

public class TaxException extends RuntimeException {

    public TaxException(String errorMsg) {
        super(errorMsg);
    }

    public TaxException(String errorMsg, Throwable exc) {
        super(errorMsg, exc);
    }
}

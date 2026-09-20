package eye.on.the.money.exception;

public class TotpException extends RuntimeException {

    public TotpException(String errorMsg) {
        super(errorMsg);
    }
}

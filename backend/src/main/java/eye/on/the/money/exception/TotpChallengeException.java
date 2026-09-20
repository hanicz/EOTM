package eye.on.the.money.exception;

public class TotpChallengeException extends RuntimeException {

    public TotpChallengeException(String errorMsg) {
        super(errorMsg);
    }
}

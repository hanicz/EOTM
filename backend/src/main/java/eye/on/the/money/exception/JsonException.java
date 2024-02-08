package eye.on.the.money.exception;

public class JsonException extends RuntimeException {
    public JsonException(String errorMsg) {
        super(errorMsg);
    }

    public JsonException(String errorMsg, Throwable exc) {
        super(errorMsg, exc);
    }
}

package eye.on.the.money.exception;

public class APIException extends RuntimeException{
    public APIException(String errorMsg){
        super(errorMsg);
    }

    public APIException(String errorMsg, Throwable exc){
        super(errorMsg, exc);
    }
}

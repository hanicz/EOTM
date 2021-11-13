package eye.on.the.money.exception;

public class APIException extends RuntimeException{
    public APIException(String errorMsg){
        super(errorMsg);
    }
}

package eye.on.the.money.exception;

public class CSVException extends RuntimeException{
    public CSVException(String errorMsg){
        super(errorMsg);
    }

    public CSVException(String errorMsg, Throwable exc){
        super(errorMsg, exc);
    }
}

package eye.on.the.money.exception;

import eye.on.the.money.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        return ResponseEntity.status(NOT_FOUND).body(new ErrorResponse(NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<ErrorResponse> handleAPIException(APIException e) {
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(CSVException.class)
    public ResponseEntity<ErrorResponse> handleCSVException(CSVException e) {
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }
}

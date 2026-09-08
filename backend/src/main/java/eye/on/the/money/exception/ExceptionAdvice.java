package eye.on.the.money.exception;

import eye.on.the.money.exception.dto.ErrorResponse;
import eye.on.the.money.logging.RequestLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
@Slf4j
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";

    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        log.warn("Not found on {}: {}", this.at(), e.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(new ErrorResponse(NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.warn("User not found on {}: {}", this.at(), e.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(new ErrorResponse(NOT_FOUND.value(), "User not found: " + e.getMessage()));
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<ErrorResponse> handleAPIException(APIException e) {
        log.warn("API error on {}: {}", this.at(), e.getMessage(), e);
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(CSVException.class)
    public ResponseEntity<ErrorResponse> handleCSVException(CSVException e) {
        log.warn("CSV error on {}: {}", this.at(), e.getMessage(), e);
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
        log.warn("Validation failed on {}: {}", this.at(), e.getMessage());
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Rejected request on {}: {}", this.at(), e.getMessage(), e);
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(TaxException.class)
    public ResponseEntity<ErrorResponse> handleTaxException(TaxException e) {
        log.warn("Tax error on {}: {}", this.at(), e.getMessage(), e);
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(FireException.class)
    public ResponseEntity<ErrorResponse> handleFireException(FireException e) {
        log.warn("FIRE error on {}: {}", this.at(), e.getMessage(), e);
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        log.warn("Sign up conflict on {}: {}", this.at(), e.getMessage());
        return ResponseEntity.status(CONFLICT).body(new ErrorResponse(CONFLICT.value(), e.getMessage()));
    }

    @ExceptionHandler(PasswordException.class)
    public ResponseEntity<ErrorResponse> handlePasswordException(PasswordException e) {
        log.warn("Password error on {}: {}", this.at(), e.getMessage());
        return ResponseEntity.status(FORBIDDEN).body(new ErrorResponse(FORBIDDEN.value(), e.getMessage()));
    }

    @ExceptionHandler(CooldownException.class)
    public ResponseEntity<ErrorResponse> handleCooldownException(CooldownException e) {
        log.warn("Cooldown active on {}: {}", this.at(), e.getMessage());
        return ResponseEntity.status(TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfter().toSeconds()))
                .body(new ErrorResponse(TOO_MANY_REQUESTS.value(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("Unhandled exception on {}", this.at(), e);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ErrorResponse(INTERNAL_SERVER_ERROR.value(), UNEXPECTED_ERROR_MESSAGE));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, @Nullable Object body, HttpHeaders headers,
                                                            HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("{} on {}: {}", statusCode, this.at(), e.getMessage(), e);
        } else {
            log.warn("{} on {}: {}", statusCode, this.at(), e.getMessage());
        }
        return super.handleExceptionInternal(e, this.errorBody(e, body, statusCode), headers, statusCode, request);
    }

    private ErrorResponse errorBody(Exception e, @Nullable Object body, HttpStatusCode statusCode) {
        if (statusCode.is5xxServerError()) {
            return new ErrorResponse(statusCode.value(), UNEXPECTED_ERROR_MESSAGE);
        }
        return new ErrorResponse(statusCode.value(), this.detailOf(e, body));
    }

    private String detailOf(Exception e, @Nullable Object body) {
        if (body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null) {
            return problemDetail.getDetail();
        }
        if (e instanceof org.springframework.web.ErrorResponse response && response.getBody().getDetail() != null) {
            return response.getBody().getDetail();
        }
        return INVALID_REQUEST_MESSAGE;
    }

    private String at() {
        String method = MDC.get(RequestLoggingFilter.METHOD);
        String path = MDC.get(RequestLoggingFilter.PATH);
        return method == null && path == null ? "(no request)" : method + " " + path;
    }
}

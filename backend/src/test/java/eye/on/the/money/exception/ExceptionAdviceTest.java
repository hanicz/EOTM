package eye.on.the.money.exception;

import eye.on.the.money.exception.dto.ErrorResponse;
import eye.on.the.money.util.Ticker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionAdviceTest {

    private final ExceptionAdvice advice = new ExceptionAdvice();

    @Test
    public void illegalArgumentMessageIsNotLeaked() {
        ResponseEntity<ErrorResponse> response =
                this.advice.handleIllegalArgumentException(new IllegalArgumentException("internal parser detail: /etc/secret"));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Invalid request", response.getBody().error());
        Assertions.assertEquals(400, response.getBody().code());
    }

    @Test
    public void numberFormatMessageIsNotLeaked() {
        IllegalArgumentException raw = assertThrows(NumberFormatException.class, () -> Integer.parseInt("abc"));

        ResponseEntity<ErrorResponse> response = this.advice.handleIllegalArgumentException(raw);

        Assertions.assertFalse(response.getBody().error().contains("For input string"));
        Assertions.assertEquals("Invalid request", response.getBody().error());
    }

    @Test
    public void validationMessageIsPreserved() {
        ValidationException e = assertThrows(ValidationException.class, () -> Ticker.normalizeExchange(" "));

        ResponseEntity<ErrorResponse> response = this.advice.handleValidationException(e);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Security exchange must not be blank", response.getBody().error());
    }

    @Test
    public void validationExceptionIsAnIllegalArgumentException() {
        Assertions.assertInstanceOf(IllegalArgumentException.class, new ValidationException("x"));
    }
}

package eye.on.the.money.exception;

import eye.on.the.money.exception.dto.ErrorResponse;
import eye.on.the.money.util.Ticker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Duration;

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

    @Test
    public void springMvcExceptionsGetTheStandardErrorBody() {
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/dividend"));

        ResponseEntity<Object> response = this.advice.handleExceptionInternal(
                new IllegalStateException("unreadable body"), null, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        Assertions.assertEquals(400, body.code());
        Assertions.assertEquals("Invalid request", body.error());
    }

    @Test
    public void springMvcExceptionsPreferTheProblemDetailBody() {
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/dividend"));
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Failed to read request");

        ResponseEntity<Object> response = this.advice.handleExceptionInternal(
                new IllegalStateException("parser blew up"), problemDetail, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        Assertions.assertEquals("Failed to read request", ((ErrorResponse) response.getBody()).error());
    }

    @Test
    public void springMvcExceptionsKeepTheirOwnSafeDetail() {
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("PUT", "/api/v1/dividend"));
        HttpRequestMethodNotSupportedException raw = new HttpRequestMethodNotSupportedException("PUT");

        ResponseEntity<Object> response = this.advice.handleExceptionInternal(
                raw, null, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, request);

        ErrorResponse body = (ErrorResponse) response.getBody();
        Assertions.assertEquals(405, body.code());
        Assertions.assertEquals(raw.getBody().getDetail(), body.error());
    }

    @Test
    public void serverSideSpringMvcExceptionsDoNotLeakTheirMessage() {
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/dividend"));

        ResponseEntity<Object> response = this.advice.handleExceptionInternal(
                new IllegalStateException("connection string: user:password@db"), null, new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        Assertions.assertEquals(500, body.code());
        Assertions.assertEquals("An unexpected error occurred", body.error());
    }

    @Test
    public void cooldownIsRejectedWithRetryAfter() {
        ResponseEntity<ErrorResponse> response = this.advice.handleCooldownException(
                new CooldownException("You can send it again in 3 hours.", Duration.ofHours(3)));

        Assertions.assertAll("Cooldown",
                () -> Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode()),
                () -> Assertions.assertEquals(429, response.getBody().code()),
                () -> Assertions.assertEquals("You can send it again in 3 hours.", response.getBody().error()),
                () -> Assertions.assertEquals("10800", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)));
    }
}

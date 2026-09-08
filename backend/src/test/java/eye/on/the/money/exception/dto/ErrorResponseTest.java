package eye.on.the.money.exception.dto;

import eye.on.the.money.logging.RequestLoggingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErrorResponseTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void picksUpTheTraceIdFromTheMdc() {
        MDC.put(RequestLoggingFilter.TRACE_ID, "abc123def456");

        ErrorResponse response = new ErrorResponse(400, "Invalid request");

        assertEquals("abc123def456", response.traceId());
        assertEquals(400, response.code());
        assertEquals("Invalid request", response.error());
    }

    @Test
    void hasNoTraceIdOutsideARequest() {
        assertNull(new ErrorResponse(500, "An unexpected error occurred").traceId());
    }

    @Test
    void keepsAnExplicitlyProvidedTraceId() {
        MDC.put(RequestLoggingFilter.TRACE_ID, "ignored12345");

        assertEquals("explicit1234", new ErrorResponse(404, "Not found", "explicit1234").traceId());
    }
}

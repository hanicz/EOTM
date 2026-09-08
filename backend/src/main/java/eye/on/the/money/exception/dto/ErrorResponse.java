package eye.on.the.money.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eye.on.the.money.logging.RequestLoggingFilter;
import org.slf4j.MDC;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int code,
        String error,
        String traceId
) {
    public ErrorResponse(int code, String error) {
        this(code, error, MDC.get(RequestLoggingFilter.TRACE_ID));
    }
}

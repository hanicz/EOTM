package eye.on.the.money.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdAndEchoesItOnTheResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dividend");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.filter.doFilter(request, response, (req, res) -> {
        });

        String traceId = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertNotNull(traceId);
        assertEquals(12, traceId.length());
    }

    @Test
    void reusesAWellFormedIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dividend");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "upstream-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.filter.doFilter(request, response, (req, res) -> {
        });

        assertEquals("upstream-42", response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER));
    }

    @Test
    void rejectsAMalformedIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dividend");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "bad id\nINFO injected line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.filter.doFilter(request, response, (req, res) -> {
        });

        assertEquals(12, response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER).length());
    }

    @Test
    void populatesMdcDuringTheChainAndClearsItAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/dividend");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, String> seen = new HashMap<>();

        this.filter.doFilter(request, response, (req, res) -> {
            seen.put(RequestLoggingFilter.TRACE_ID, MDC.get(RequestLoggingFilter.TRACE_ID));
            MDC.put(RequestLoggingFilter.USER_ID, "7");
        });

        assertNotNull(seen.get(RequestLoggingFilter.TRACE_ID));
        assertNull(MDC.get(RequestLoggingFilter.TRACE_ID));
        assertNull(MDC.get(RequestLoggingFilter.USER_ID));
    }

    @Test
    void clearsMdcWhenTheChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dividend");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        assertThrows(IllegalStateException.class, () -> this.filter.doFilter(request, response, failing));

        assertNull(MDC.get(RequestLoggingFilter.TRACE_ID));
    }

    @Test
    void skipsStaticAndActuatorPaths() {
        assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest("GET", "/favicon.ico")));
        assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest("GET", "/")));
        assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest("GET", "/index.html")));
        assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest("GET", "/resources/main.js")));
        assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health")));
        assertFalse(this.filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/v1/dividend")));
    }
}
